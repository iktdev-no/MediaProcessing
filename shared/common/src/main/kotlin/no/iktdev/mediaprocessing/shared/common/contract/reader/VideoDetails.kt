package no.iktdev.mediaprocessing.shared.common.contract.reader

data class VideoDetails(
    val serieInfo: SerieInfo? = null,
    val type: String,
    val fileName: String
)

data class SerieInfo(
    val episodeTitle: String? = null,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val title: String
)
