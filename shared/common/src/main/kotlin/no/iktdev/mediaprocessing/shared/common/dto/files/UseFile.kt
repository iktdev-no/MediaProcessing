package no.iktdev.mediaprocessing.shared.common.dto.files

import java.io.File

class UseFile(path: String) : IFile {

    private val file = File(path)

    override val path: String
        get() = file.path

    override val name: String
        get() = file.name

    override val nameWithoutExtension: String
        get() = name.substringAfterLast('.', missingDelimiterValue = "")

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
}
