package no.iktdev.mediaprocessing.shared.common.dto.files

import java.io.File

class FakeFile(
    override val path: String,
    private val exists: Boolean = true,
    private val size: Long = 0,
    private val directory: Boolean = false,
    private val children: List<IFile> = emptyList()
) : IFile {

    override val name: String
        get() = path.substringAfterLast('/', path)

    override val parent: String?
        get() = path.substringBeforeLast('/', missingDelimiterValue = "").ifEmpty { null }

    override val absolutePath: String
        get() = path

    override val nameWithoutExtension: String
        get() = name.substringAfterLast('.', missingDelimiterValue = "")

    override val parentFile: IFile
        get() = IFile(parent!!)

    override fun exists(): Boolean = exists

    override fun isFile(): Boolean = !directory

    override fun isDirectory(): Boolean = directory

    override fun length(): Long = size

    override fun delete(): Boolean = true  // always succeeds in tests

    override fun listFiles(): List<IFile> = children

    override fun toJavaFile(): File =
        File(path) // rarely used in tests, but provided for interface completeness
}
