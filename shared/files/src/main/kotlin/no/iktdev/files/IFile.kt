package no.iktdev.files

import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import java.nio.file.Paths

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

    fun listFiles(): List<IFile>

    fun toJavaFile(): File

    fun readText(): String

    fun writeText(text: String)
    fun appendLine(text: String)

    fun mkdirs(): Boolean
    fun mkdir(): Boolean

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

    fun resolve(child: String): IFile {
        return IFile(Paths.get(this.path).resolve(child).toString())
    }

    fun renameTo(dest: IFile): Boolean

}
