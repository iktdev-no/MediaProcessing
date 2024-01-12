package no.iktdev.mediaprocessing.shared.contract.reader

data class MetadataDto(
    val title: String,
    val collection: String,
    val type: String,
    val cover: MetadataCoverDto?,
    val summary: List<SummaryInfo> = emptyList(),
    val genres: List<String>,
)

data class SummaryInfo(
    val summary: String,
    val language: String = "eng"
)

data class MetadataCoverDto(
    val cover: String?, // ex Fancy.jpeg
    val coverUrl: String?,
    val coverFile: String?
)