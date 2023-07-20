package no.iktdev.streamit.content.common.dto

data class Metadata(
    val title: String,
    val altTitle: List<String> = emptyList(),
    val cover: String? = null,
    val type: String,
    val summary: String? = null,
    val genres: List<String> = emptyList()
)
