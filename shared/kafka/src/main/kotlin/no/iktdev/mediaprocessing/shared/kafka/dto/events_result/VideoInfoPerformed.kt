package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status

data class VideoInfoPerformed(
    override val status: Status,
    val data: VideoInfo
)
    : MessageDataWrapper(status)


data class EpisodeInfo(
    val title: String,
    val episode: Int,
    val season: Int,
    val episodeTitle: String?,
    override val fullName: String
): VideoInfo(fullName)

data class MovieInfo(
    val title: String,
    override val fullName: String
) : VideoInfo(fullName)

data class SubtitleInfo(
    val inputFile: String,
    val collection: String,
    val language: String
)

abstract class VideoInfo(
    @Transient open val fullName: String
)