package no.iktdev.files

import net.jpountz.xxhash.XXHashFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

interface IFile {
    val path: String
    val name: String
    val parent: String?
    val parentFile: IFile
    val absolutePath: String

    val nameWithoutExtension: String

    fun exists(): Boolean
    fun isFile(): Boolean
    fun isDirectory(): Boolean
    fun length(): Long
    fun delete(): Boolean

    fun canRead(): Boolean
    fun canWrite(): Boolean

    fun listFiles(): List<IFile>

    fun listFiles(filter: (parent: IFile, name: String) -> Boolean): List<IFile> {
        return this.listFiles()
            .filter { child -> filter(this, child.name) }
    }

    fun toPath(): Path = Paths.get(this.absolutePath)

    fun toJavaFile(): File

    fun readText(): String

    fun writeText(text: String)
    fun appendLine(text: String)

    fun mkdirs(): Boolean
    fun mkdir(): Boolean

    fun setWritable(state: Boolean): Boolean

    companion object {
        // Default factory – can be overridden in tests
        var factory: (String) -> IFile = { UseFile(it) }

        operator fun invoke(path: String): IFile = factory(path)
    }

    fun using(vararg paths: String): IFile =
        IFile(paths.fold(Paths.get(path), Path::resolve).toString())

    fun extension(): String =
        name.substringAfterLast('.', "")

    fun walk(): Sequence<IFile>
    fun lastModified(): Long
    fun printWriter(): PrintWriter

    fun deleteRecursively(): Boolean
    fun deleteAllChildren(): Boolean

    fun resolve(child: String): IFile {
        return IFile(Paths.get(this.path).resolve(child).toString())
    }

    fun renameTo(dest: IFile): Boolean

    fun notExist(): Boolean {
        return !exists()
    }

    fun copyTo(dest: IFile, overwrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE): IFile

    fun openInputStream(): InputStream
    fun openOutputStream(): OutputStream
    fun writeBytes(bytes: ByteArray)
    fun setLastModified(time: Long)
    fun startsWith(other: IFile): Boolean

    fun toSha256(): FileHash {
        val digest = MessageDigest.getInstance("SHA-256")
        this.openInputStream().use { input ->
            val buffer = ByteArray(1024 * 1024) // 1 MB buffer
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return FileHash(
            digest.digest().joinToString("") { "%02x".format(it) },
            FileHashType.SHA256
        )
    }

    fun toXxHash(): FileHash {
        val factory = XXHashFactory.nativeInstance()
        val hasher = factory.hash64()

        // Merk: XXH3 er enklest å bruke på hele bytes,
        // men for strømming bruker vi en 'streaming' hasher:
        val streamHasher = factory.newStreamingHash64(0) // 0 er seed

        this.openInputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                streamHasher.update(buffer, 0, bytesRead)
            }
        }

        // Returnerer som hex-streng
        return FileHash(java.lang.Long.toHexString(streamHasher.value), FileHashType.XX64Hash)
    }
}
