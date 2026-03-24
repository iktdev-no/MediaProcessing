package no.iktdev.mediaprocessing.ffmpeg.model

import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec

data class AudioTarget(
    val listIndex: Int,
    val ffmpegIndex: Int,
    val codec: AudioCodec,
    val meta: AudioTargetMeta? = null
)

data class AudioTargetMeta(
    val language: String? = null,
) {}

data class VideoTarget(
    val listIndex: Int,
    val ffmpegIndex: Int,
    val codec: VideoCodec
)


data class SelectedAudioTracks(
    val defaultListIndex: Int,
    val defaultFfmpegIndex: Int,
    val extendedListIndex: Int?,
    val extendedFfmpegIndex: Int?
)