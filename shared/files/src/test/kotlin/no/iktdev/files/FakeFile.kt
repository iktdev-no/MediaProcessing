package no.iktdev.files

import java.io.File
import java.io.PrintWriter
import java.nio.file.Paths

class FakeFile(
    override val path: String,
    private var exists: Boolean = true,
    private var size: Long = 0,
    private var directory: Boolean = false,
    private val children: MutableList<IFile> = mutableListOf(),
    private var content: String = "",
    private var modified: Long = System.currentTimeMillis()
) : IFile {

    companion object {
        var fileRegistry: MutableMap<String, IFile> = mutableMapOf()

        fun getOrCreate(path: String): IFile =
            fileRegistry.getOrPut(path) { FakeFile(path) }
    }

    override val name: String
        get() = path.substringAfterLast('/', path)

    override val parent: String?
        get() = path.substringBeforeLast('/', missingDelimiterValue = "").ifEmpty { null }

    override val absolutePath: String
        get() = path

    override val nameWithoutExtension: String
        get() = name.substringBeforeLast('.', name)

    override val parentFile: IFile
        get() = parent?.let { IFile(it) } ?: FakeFile("")

    override fun exists(): Boolean = exists
    fun changeExist(state: Boolean) {
        exists = state
    }

    override fun isFile(): Boolean = !directory

    override fun isDirectory(): Boolean = directory

    override fun length(): Long = size

    override fun delete(): Boolean {
        exists = false
        content = ""
        return true
    }

    override fun listFiles(): List<IFile> = children.toList()

    override fun toJavaFile(): File =
        throw UnsupportedOperationException("FakeFile does not support toJavaFile()")

    override fun readText(): String = content

    override fun writeText(text: String) {
        exists = true
        content = text
        size = content.length.toLong()
        modified = System.currentTimeMillis()
    }

    override fun appendLine(text: String) {
        exists = true
        content += "$text\n"
        size = content.toByteArray().size.toLong()
        modified = System.currentTimeMillis()
    }

    override fun mkdir(): Boolean {
        exists = true
        directory = true
        return true
    }

    override fun mkdirs(): Boolean {
        exists = true
        directory = true
        return true
    }


    override fun lastModified(): Long = modified
    fun setLastModified(value: Long) {
        modified = value
    }


    override fun printWriter(): PrintWriter {
        return object : PrintWriter(object : java.io.Writer() {

            override fun write(str: String) {
                if (str.isEmpty()) return
                exists = true
                content += str
                size = content.toByteArray().size.toLong()
                modified = System.currentTimeMillis()
            }

            override fun write(cbuf: CharArray?, off: Int, len: Int) {
                if (cbuf == null || len <= 0) return
                val str = String(cbuf, off, len)
                write(str)
            }

            override fun write(c: Int) {
                write(c.toChar().toString())
            }

            override fun flush() { /* no-op */ }
            override fun close() { /* no-op */ }

        }) {}
    }



    override fun walk(): Sequence<IFile> =
        fileRegistry.values
            .filter { it.absolutePath.startsWith(this.absolutePath) }
            .asSequence()


    override fun deleteRecursively(): Boolean {
        val prefix = this.absolutePath

        fileRegistry.values
            .filter { it.absolutePath.startsWith(prefix) }
            .forEach {
                if (it is FakeFile) {
                    it.exists = false
                    it.content = ""
                }
            }

        return true
    }


    override fun using(vararg paths: String): IFile {
        val newPath = Paths.get(this.absolutePath, *paths).normalize().toString()

        // Return existing instance if present
        fileRegistry[newPath]?.let { return it }

        // Create new FakeFile and register it
        val child = FakeFile(newPath)
        fileRegistry[newPath] = child

        // Mark parent as directory
        this.directory = true

        return child
    }


}