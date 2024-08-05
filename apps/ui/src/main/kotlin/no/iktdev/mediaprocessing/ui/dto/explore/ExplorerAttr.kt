package no.iktdev.mediaprocessing.ui.dto.explore

interface ExplorerAttr {
    val created: Long
}

data class ExplorerAttributes(
    override val created: Long
): ExplorerAttr