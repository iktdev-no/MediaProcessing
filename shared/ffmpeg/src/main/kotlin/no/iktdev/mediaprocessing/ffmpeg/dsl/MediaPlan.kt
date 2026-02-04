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

        // -----------------------------
        // VIDEO
        // -----------------------------
        val vStream = videoStreams[videoTrack.listIndex]
        val vDecision = videoTrack.codec.determineTranscodeDecision(vStream)

        // FFmpeg mapping bruker ffmpegIndex
        args += listOf("-map", "0:v:${videoTrack.ffmpegIndex}")

        args += when (vDecision) {
            TranscodeDecision.Copy -> listOf("-c:v", "copy")
            TranscodeDecision.Remux -> listOf("-c:v", videoTrack.codec.codec)
            TranscodeDecision.Reencode -> videoTrack.codec.buildFfmpegArgs(vStream)
        }

        // -----------------------------
        // AUDIO
        // -----------------------------
        // Fjern duplikate spor (samme listIndex, ffmpegIndex og codec-type)
        val uniqueAudioTargets = audioTracks
            .distinctBy { Triple(it.listIndex, it.ffmpegIndex, it.codec::class) }

        uniqueAudioTargets.forEachIndexed { outIdx, target ->

            val aStream = audioStreams[target.listIndex]
            val aDecision = target.codec.determineTranscodeDecision(aStream)


            // FFmpeg mapping bruker ffmpegIndex
            args += listOf("-map", "0:a:${target.ffmpegIndex}")

            when (aDecision) {
                TranscodeDecision.Copy ->
                    args += listOf("-c:a:$outIdx", "copy")

                TranscodeDecision.Remux ->
                    args += listOf("-c:a:$outIdx", target.codec.codec)

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
            (videoCodec is VideoCodec.H264 || videoCodec is VideoCodec.Hevc) &&
                    audioCodecs.all { it is AudioCodec.Aac || it is AudioCodec.Mp3 } -> "mp4"

            (videoCodec is VideoCodec.Vp8 || videoCodec is VideoCodec.Vp9 || videoCodec is VideoCodec.Av1) &&
                    audioCodecs.all { it is AudioCodec.Opus || it is AudioCodec.Vorbis } -> "webm"

            else -> "mkv"
        }
    }
}


data class AudioTarget(
    val listIndex: Int,
    val ffmpegIndex: Int,
    val codec: AudioCodec
)

data class VideoTarget(
    val listIndex: Int,
    val ffmpegIndex: Int,
    val codec: VideoCodec
)

