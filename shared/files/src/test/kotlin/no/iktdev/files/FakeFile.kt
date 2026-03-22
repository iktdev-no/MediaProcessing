package no.iktdev.files

import java.io.File
import java.io.PrintWriter
import java.nio.file.Paths

class FakeFile(
    override var path: String,
    private var exists: Boolean = true,
    private var size: Long = 0,
    private var directory: Boolean = false,
    private val children: MutableList<IFile> = mutableListOf(),
    private var content: String = "",
    private var modified: Long = System.currentTimeMillis()
) : IFile {

    init {
        // Register this instance if not already present
        fileRegistry.putIfAbsent(path, this)
    }

    companion object {
        val fileRegistry: MutableMap<String, FakeFile> = mutableMapOf()

        fun wipe() = fileRegistry.clear()

        fun getOrCreate(path: String): FakeFile =
            fileRegistry.getOrPut(path) { FakeFile(path) }
    }

    /** Always return the canonical instance for this path */
    private fun ref(): FakeFile = fileRegistry[path] ?: this

    override val name: String
        get() = path.substringAfterLast('/')

    override val parent: String?
        get() = path.substringBeforeLast('/', "").ifEmpty { null }

    override val absolutePath: String
        get() = path

    override val nameWithoutExtension: String
        get() = name.substringBeforeLast('.', name)

    override val parentFile: IFile
        get() = parent?.let { IFile(it) } ?: FakeFile("")

    // --- State accessors always use registry instance ---
    override fun exists(): Boolean = ref().exists
    fun changeExist(exists: Boolean) {
        val r = ref()
        r.exists = exists
    }
    override fun isFile(): Boolean = !ref().directory
    override fun isDirectory(): Boolean = ref().directory
    override fun length(): Long = ref().size
    override fun readText(): String = ref().content
    override fun lastModified(): Long = ref().modified

    override fun writeText(text: String) {
        val r = ref()
        r.exists = true
        r.content = text
        r.size = text.toByteArray().size.toLong()
        r.modified = System.currentTimeMillis()
    }

    override fun appendLine(text: String) {
        val r = ref()
        r.exists = true
        r.content += "$text\n"
        r.size = r.content.toByteArray().size.toLong()
        r.modified = System.currentTimeMillis()
    }

    override fun mkdir(): Boolean {
        val r = ref()
        r.exists = true
        r.directory = true
        return true
    }

    override fun mkdirs(): Boolean = mkdir()

    override fun delete(): Boolean {
        val r = ref()
        r.exists = false
        r.content = ""
        r.size = 0
        return true
    }

    override fun deleteRecursively(): Boolean {
        val prefix = absolutePath
        fileRegistry.values
            .filter { it.absolutePath.startsWith(prefix) }
            .forEach {
                it.exists = false
                it.content = ""
                it.size = 0
            }
        return true
    }

    override fun listFiles(): List<IFile> =
        fileRegistry.values
            .filter { it.absolutePath.startsWith("$absolutePath/") }
            .map { it }

    override fun walk(): Sequence<IFile> =
        fileRegistry.values
            .filter { it.absolutePath.startsWith(absolutePath) }
            .asSequence()

    override fun printWriter(): PrintWriter =
        object : PrintWriter(object : java.io.Writer() {
            override fun write(str: String) {
                if (str.isEmpty()) return
                val r = ref()
                r.exists = true
                r.content += str
                r.size = r.content.toByteArray().size.toLong()
                r.modified = System.currentTimeMillis()
            }

            override fun write(cbuf: CharArray?, off: Int, len: Int) {
                if (cbuf != null && len > 0) write(String(cbuf, off, len))
            }

            override fun write(c: Int) = write(c.toChar().toString())
            override fun flush() {}
            override fun close() {}
        }) {}


    override fun toJavaFile(): File =
        throw UnsupportedOperationException("FakeFile does not support toJavaFile()")

    override fun using(vararg paths: String): IFile {
        val newPath = Paths.get(absolutePath, *paths).normalize().toString()
        return getOrCreate(newPath)
    }

    override fun renameTo(dest: IFile): Boolean {
        val src = ref()
        if (!src.exists) return false

        val oldPath = src.path
        val destPath = dest.absolutePath

        // Remove any existing dest
        fileRegistry.remove(destPath)

        // Move registry entry
        fileRegistry[destPath] = src
        src.path = destPath

        // Mark old path as deleted (but do NOT modify src)
        fileRegistry[oldPath] = FakeFile(oldPath, exists = false, content = "", size = 0)

        return true
    }


}
