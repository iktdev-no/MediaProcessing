package no.iktdev.mediaprocessing.ui.models.contract.files

enum class FileType {
    Folder,
    File
}

enum class FileAccessMode {
    READ_WRITE,
    READ_ONLY,
    NO_ACCESS
}

sealed class UiFile {
    abstract val name: String
    abstract val uri: String
    abstract val created: Long
    abstract val type: FileType
    abstract val actions: FileActions
    abstract val accessMode: FileAccessMode
}

data class File(
    override val name: String,
    override val uri: String,
    override val created: Long,
    val extension: String,
    override val actions: FileActions,
    val size: Long,
    override val accessMode: FileAccessMode
) : UiFile() {
    override val type = FileType.File
}

data class Folder(
    override val name: String,
    override val uri: String,
    override val created: Long,
    override val actions: FileActions = FileActions(emptyList(), listOf(
        FileAction(id = FileActionType.Delete, requiresConfirmation = true)
    )),
    override val accessMode: FileAccessMode

) : UiFile() {
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

