package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status

data class VideoInfoPerformed(
    override val status: Status,
    val info: VideoInfo
)
    : MessageDataWrapper(status)


data class EpisodeInfo(
    override val type: String,
    val title: String,
    val episode: Int,
    val season: Int,
    val episodeTitle: String?,
    override val fullName: String
): VideoInfo(type, fullName)

data class MovieInfo(
    override val type: String,
    val title: String,
    override val fullName: String
) : VideoInfo(type, fullName)

data class SubtitleInfo(
    val inputFile: String,
    val collection: String,
    val language: String
)

open class VideoInfo(
    @Transient open val type: String,
    @Transient open val fullName: String
)