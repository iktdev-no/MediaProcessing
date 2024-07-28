package no.iktdev.mediaprocessing.shared.common.contract.reader

data class SubtitlesDto(
    val collection: String,
    val language: String,
    val subtitleFile: String,
    val format: String,
    val associatedWithVideo: String
)