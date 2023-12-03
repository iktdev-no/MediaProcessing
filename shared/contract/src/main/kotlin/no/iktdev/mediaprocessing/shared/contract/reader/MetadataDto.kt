package no.iktdev.mediaprocessing.shared.contract.reader

data class MetadataDto(
    val title: String,
    val type: String,
    val cover: MetadataCoverDto,
    val summary: String,
    val genres: List<String>
)

data class MetadataCoverDto(
    val cover: String,
    val coverUrl: String,
    val coverFile: String
)