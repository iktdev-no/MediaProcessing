package no.iktdev.mediaprocessing.shared.contract.reader

data class MetadataDto(
    val title: String,
    val collection: String,
    val type: String,
    val cover: String?,
    val summary: List<SummaryInfo> = emptyList(),
    val genres: List<String>,
    val titles: List<String> = emptyList()
)

data class SummaryInfo(
    val summary: String,
    val language: String = "eng"
)