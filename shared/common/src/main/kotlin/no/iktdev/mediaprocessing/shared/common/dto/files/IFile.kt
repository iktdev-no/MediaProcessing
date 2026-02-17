package no.iktdev.mediaprocessing.shared.common.dto.files

interface IFile {
    val path: String
    val name: String
    val parent: String?
    val absolutePath: String

    fun exists(): Boolean
    fun isFile(): Boolean
    fun isDirectory(): Boolean
    fun length(): Long
    fun delete(): Boolean

    fun listFiles(): List<IFile>

    fun toJavaFile(): java.io.File


    companion object {
        // Default factory – can be overridden in tests
        var factory: (String) -> IFile = { UseFile(it) }

        operator fun invoke(path: String): IFile = factory(path)
    }

}
