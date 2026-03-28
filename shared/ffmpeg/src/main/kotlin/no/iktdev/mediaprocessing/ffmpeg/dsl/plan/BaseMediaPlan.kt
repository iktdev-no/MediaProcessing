package no.iktdev.mediaprocessing.ffmpeg.dsl.plan

import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.model.AudioTarget
import no.iktdev.mediaprocessing.ffmpeg.model.VideoTarget
import no.iktdev.mediaprocessing.ffmpeg.util.FfmpegCodecs
import org.jetbrains.annotations.VisibleForTesting

open class BaseMediaPlan(
    val sourceVideoCodec: FfmpegCodecs,
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
        val effectiveVideoCodec = when (videoTrack.codec) {
            is VideoCodec.Copy -> VideoCodec.fromFFmpegCodec(sourceVideoCodec)
            else -> videoTrack.codec
        }

        val audioCodecs = audioTracks.map { it.codec }

        return when {
            (effectiveVideoCodec is VideoCodec.H264 || effectiveVideoCodec is VideoCodec.Hevc) &&
                    audioCodecs.all { it is AudioCodec.Aac || it is AudioCodec.Mp3 } -> "mp4"

            (effectiveVideoCodec is VideoCodec.Vp8 || effectiveVideoCodec is VideoCodec.Vp9 || effectiveVideoCodec is VideoCodec.Av1) &&
                    audioCodecs.all { it is AudioCodec.Opus || it is AudioCodec.Vorbis } -> "webm"

            else -> "mkv"
        }
    }

}


