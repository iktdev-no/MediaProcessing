package no.iktdev.streamit.content.common.dto.reader

data class EpisodeInfo(
    val title: String,
    val episode: Int,
    val season: Int,
    val episodeTitle: String?,
    override val fullName: String
): VideoInfo(fullName)