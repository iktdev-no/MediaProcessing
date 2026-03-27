package no.iktdev.files

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter

class UseFile(path: String) : IFile {

    private val file = File(path)

    override val path: String
        get() = file.path

    override val name: String
        get() = file.name

    override val nameWithoutExtension: String
        get() = name.substringBeforeLast('.', missingDelimiterValue = "")

    override val parent: String?
        get() = file.parent

    override val absolutePath: String
        get() = file.absolutePath

    override val parentFile: IFile
        get() = IFile(parent!!)

    override fun exists(): Boolean =
        file.exists()

    override fun isFile(): Boolean =
        file.isFile

    override fun isDirectory(): Boolean =
        file.isDirectory

    override fun length(): Long =
        file.length()

    override fun delete(): Boolean =
        file.delete()

    override fun listFiles(): List<IFile> =
        file.listFiles()?.map { UseFile(it.path) } ?: emptyList()

    override fun toJavaFile(): File =
        file

    override fun readText(): String {
        return file.readText()
    }

    override fun writeText(text: String) {
        return file.writeText(text)
    }

    override fun appendLine(text: String) {
        file.parentFile.mkdirs()
        FileOutputStream(file, true).bufferedWriter(Charsets.UTF_8).use {
            it.appendLine(text)
        }
    }

    override fun mkdirs(): Boolean {
        return file.mkdirs()
    }

    override fun mkdir(): Boolean {
        return file.mkdir()
    }

    override fun walk(): Sequence<IFile> =
        file.walk().map { UseFile(it.path) }


    override fun lastModified(): Long {
        return file.lastModified()
    }

    override fun printWriter(): PrintWriter {
        return file.printWriter()
    }

    override fun deleteRecursively(): Boolean {
        return file.deleteRecursively()
    }

    override fun renameTo(dest: IFile): Boolean {
        return file.renameTo(dest.toJavaFile())
    }

    override fun setWritable(state: Boolean): Boolean {
        return file.setWritable(state)
    }

    override fun canRead() = file.canRead()

    override fun copyTo(dest: IFile, overwrite: Boolean, bufferSize: Int): IFile {
        file.copyTo(dest.toJavaFile(), overwrite, bufferSize)
        return dest
    }

    override fun openInputStream(): InputStream  = file.inputStream()
    override fun openOutputStream(): OutputStream  = file.outputStream()
}
