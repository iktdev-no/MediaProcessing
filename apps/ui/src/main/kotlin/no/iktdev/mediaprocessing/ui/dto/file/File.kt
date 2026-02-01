package no.iktdev.mediaprocessing.ui.dto.file

enum class FileType {
    Folder,
    File
}

sealed class IFile {
    abstract val name: String
    abstract val uri: String
    abstract val created: Long
    abstract val type: FileType
    abstract val actions: FileActions
}

data class FileItem(
    override val name: String,
    override val uri: String,
    override val created: Long,
    val extension: String,
    override val actions: FileActions
) : IFile() {
    override val type = FileType.File
}

data class FolderItem(
    override val name: String,
    override val uri: String,
    override val created: Long,
    override val actions: FileActions = FileActions(emptyList(), listOf(
        FileAction(id = FileActionType.Delete, requiresConfirmation = true)
    )),
) : IFile() {
    override val type = FileType.Folder
}

data class FileActions(
    val mediaActions: List<MediaAction>,
    val fileActions: List<FileAction>
)

data class MediaAction(
    val id: MediaActionType,
    val title: String = id.label
) {
}

enum class MediaActionType(val label: String) {
    All("All"),
    Encode("Encode"),
    ExtractSubtitles("Extract Subtitles"),
    ConvertSubtitle("Convert Subtitle"),
    MetadataSearch("Search for metadata"),
}

enum class FileActionType(val label: String) {
    Open("Open"),
    Delete("Delete")
}

data class FileAction(
    val id: FileActionType,
    val title: String = id.label,
    val requiresConfirmation: Boolean = false
)

