package no.iktdev.streamit.content.common.dto.reader

data class MovieInfo(
    val title: String,
    override val fullName: String
) : VideoInfo(fullName)