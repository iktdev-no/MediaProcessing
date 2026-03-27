package no.iktdev.files

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.Paths

class FakeFile(
    override var path: String,
    private var exists: Boolean = true,
    private var size: Long = 0,
    private var directory: Boolean = false,
    private var content: String = "",
    private var modified: Long = System.currentTimeMillis(),
    private var writable: Boolean = true
) : IFile {

    init {
        if (!directory) {
            directory = !path.substringAfterLast('/').contains('.')
        }
        fileRegistry.putIfAbsent(path, this)
    }


    companion object {
        val fileRegistry: MutableMap<String, FakeFile> = mutableMapOf()

        fun wipe() = fileRegistry.clear()

        fun getOrCreate(path: String): FakeFile =
            fileRegistry.getOrPut(path) {
                FakeFile(path)
            }
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
    override fun canRead(): Boolean {
        return true
    }

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

    fun setDirectory() {
        ref().directory = true
    }

    fun setFile() {
        ref().directory = false
    }

    override fun mkdir(): Boolean {
        val r = ref()
        r.exists = true
        r.directory = true
        return true
    }

    override fun setWritable(state: Boolean): Boolean {
        val r = ref()
        return r.setWritable(state)
    }

    override fun mkdirs(): Boolean {
        return mkdir()
    }

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

    override fun openInputStream(): InputStream {
        val r = ref()
        val bytes = r.content.toByteArray()
        return object : InputStream() {
            var pos = 0

            override fun read(): Int {
                return if (pos < bytes.size) {
                    bytes[pos++].toInt() and 0xFF
                } else {
                    -1
                }
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (pos >= bytes.size) return -1
                val toCopy = minOf(len, bytes.size - pos)
                System.arraycopy(bytes, pos, b, off, toCopy)
                pos += toCopy
                return toCopy
            }
        }
    }

    override fun openOutputStream(): OutputStream {
        val r = ref()
        r.exists = true

        return object : OutputStream() {
            private val buffer = StringBuilder()

            override fun write(b: Int) {
                buffer.append(b.toChar())
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                buffer.append(String(b, off, len))
            }

            override fun flush() {
                val newContent = buffer.toString()
                r.content += newContent
                r.size = r.content.toByteArray().size.toLong()
                r.modified = System.currentTimeMillis()
            }

            override fun close() {
                flush()
            }
        }
    }

    override fun copyTo(dest: IFile, overwrite: Boolean, bufferSize: Int): IFile {
        if (!this.exists()) {
            throw RuntimeException("${this.absolutePath} The source file doesn't exist.")
        }

        if (dest.exists()) {
            if (!overwrite)
                throw FileAlreadyExistsException(
                    this.absolutePath,
                    dest.absolutePath,
                    "The destination file already exists."
                )
            else if (!dest.delete())
                throw FileAlreadyExistsException(
                    this.absolutePath,
                    dest.absolutePath,
                    "Tried to overwrite the destination, but failed to delete it."
                )
        }

        if (this.isDirectory()) {
            if (!dest.mkdirs())
                throw FileSystemException(this.absolutePath, dest.absolutePath, "Failed to create target directory.")
        } else {
            dest.parentFile.mkdirs()

            this.openInputStream().use { input ->
                dest.openOutputStream().use { output ->
                    val buffer = ByteArray(bufferSize)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
        }

        return dest
    }



    override fun equals(other: Any?): Boolean {
        return if (other is FakeFile) {
            this.absolutePath == other.absolutePath
        } else if (other is IFile) {
            other is IFile && this.absolutePath == other.absolutePath
        } else {
            super.equals(other)
        }
    }

}
