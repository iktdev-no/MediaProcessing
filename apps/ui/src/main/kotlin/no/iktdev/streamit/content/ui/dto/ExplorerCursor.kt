package no.iktdev.streamit.content.ui.dto

data class ExplorerCursor (
    val name: String,
    val path: String,
    val items: List<ExplorerItem>,
)
enum class ExplorerItemType {
    FILE,
    FOLDER
}

data class ExplorerItem(
    val name: String,
    val path: String,
    val extension: String? = null,
    val created: Long,
    val type: ExplorerItemType
)