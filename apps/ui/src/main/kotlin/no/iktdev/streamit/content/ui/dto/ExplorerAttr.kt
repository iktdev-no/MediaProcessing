package no.iktdev.streamit.content.ui.dto

interface ExplorerAttr {
    val created: Long
}

data class ExplorerAttributes(
    override val created: Long
): ExplorerAttr