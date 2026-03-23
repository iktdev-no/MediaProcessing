package no.iktdev.mediaprocessing.ffmpeg.dsl.plan

import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.model.AudioTarget
import no.iktdev.mediaprocessing.ffmpeg.model.VideoTarget
import org.jetbrains.annotations.VisibleForTesting

open class BaseMediaPlan(
    val videoTrack: VideoTarget,
    val audioTracks: List<AudioTarget>
) {

    @VisibleForTesting
    internal fun getUsableAudioTargetedTracks(): List<AudioTarget> {
        return audioTracks.distinctBy {
            Triple(it.ffmpegIndex, it.codec::class, it.codec.channels)
        }
    }


    fun toContainer(): String {
        val videoCodec = videoTrack.codec
        val audioCodecs = audioTracks.map { it.codec }

        return when {
            (videoCodec is VideoCodec.H264 || videoCodec is VideoCodec.Hevc) &&
                    audioCodecs.all { it is AudioCodec.Aac || it is AudioCodec.Mp3 } -> "mp4"

            (videoCodec is VideoCodec.Vp8 || videoCodec is VideoCodec.Vp9 || videoCodec is VideoCodec.Av1) &&
                    audioCodecs.all { it is AudioCodec.Opus || it is AudioCodec.Vorbis } -> "webm"

            else -> "mkv"
        }
    }
}


