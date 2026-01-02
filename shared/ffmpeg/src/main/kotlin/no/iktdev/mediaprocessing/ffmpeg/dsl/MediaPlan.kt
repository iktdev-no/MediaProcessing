package no.iktdev.mediaprocessing.ffmpeg.dsl

import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream

// DSL entrypoint
data class MediaPlan(
    val videoTrack: VideoTarget,
    val audioTracks: MutableList<AudioTarget> = mutableListOf()
) {
    fun toFfmpegArgs(
        videoStreams: List<VideoStream>,
        audioStreams: List<AudioStream>
    ): List<String> {
        val args = mutableListOf<String>()


        // Video
        val vStream = videoStreams[videoTrack.index]
        val vDecision = videoTrack.codec.determineTranscodeDecision(vStream)
        args += listOf("-map", "0:v:${videoTrack.index}")
        args += when (vDecision) {
            TranscodeDecision.Copy -> listOf("-c:v", "copy")
            TranscodeDecision.Remux -> listOf("-c:v", videoTrack.codec.codec)
            TranscodeDecision.Reencode -> videoTrack.codec.buildFfmpegArgs(vStream)
        }

        // Audio
        audioTracks.forEachIndexed { outIdx, target ->
            val aStream = audioStreams[target.index]
            val aDecision = target.codec.determineTranscodeDecision(aStream)

            if (outIdx > 0) {
                val otherIsCopy = audioTracks.filter { it -> it.index == target.index && it !== target }.any { it.codec == AudioCodec.Copy }
                if (otherIsCopy && aDecision == TranscodeDecision.Copy) {
                    // Hvis en annen audio-track med samme index er satt til copy, kan vi ikke copy denne også
                    return@forEachIndexed
                }
            }

            args += listOf("-map", "0:a:${target.index}")
            when (aDecision) {
                TranscodeDecision.Copy -> args += listOf("-c:a:$outIdx", "copy")
                TranscodeDecision.Remux -> args += listOf("-c:a:$outIdx", target.codec.codec)
                TranscodeDecision.Reencode -> {
                    val built = target.codec.buildFfmpegArgs(aStream).toMutableList()
                    // injiser output-indeks i alle audio-flagg
                    for (i in built.indices) {
                        if (built[i] == "-c:a") built[i] = "-c:a:$outIdx"
                        if (built[i] == "-b:a") built[i] = "-b:a:$outIdx"
                        if (built[i] == "-ar") built[i] = "-ar:$outIdx"
                        if (built[i] == "-ac") built[i] = "-ac:$outIdx"
                    }
                    args += built
                }
            }
        }
        return args
    }

    fun toContainer(): String {
        val videoCodec = videoTrack.codec
        val audioCodecs = audioTracks.map { it.codec }

        return when {
            // MP4: H.264/HEVC + AAC/MP3
            (videoCodec is VideoCodec.H264 || videoCodec is VideoCodec.Hevc) &&
                    audioCodecs.all { it is AudioCodec.Aac || it is AudioCodec.Mp3 } -> "mp4"

            // WEBM: VP8/VP9/AV1 + Opus/Vorbis
            (videoCodec is VideoCodec.Vp8 || videoCodec is VideoCodec.Vp9 || videoCodec is VideoCodec.Av1) &&
                    audioCodecs.all { it is AudioCodec.Opus || it is AudioCodec.Vorbis } -> "webm"

            // Fallback: MKV (støtter nesten alt)
            else -> "mkv"
        }
    }
}

// Video target: index + codec
data class VideoTarget(
    val index: Int,
    val codec: VideoCodec
)

// Audio target: index + codec
data class AudioTarget(
    val index: Int,
    val codec: AudioCodec
)
