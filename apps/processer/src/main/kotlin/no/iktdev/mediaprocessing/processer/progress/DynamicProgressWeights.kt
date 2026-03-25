package no.iktdev.mediaprocessing.processer.progress

import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec

class DynamicProgressWeights(
    private val video: FFmpegInstructions,
    private val audio: List<FFmpegInstructions>
) {

    data class Weights(
        val video: Double,
        val audioPerTrack: List<Double>,
        val merge: Double
    )

    fun compute(): Weights {

        // -----------------------------
        // VIDEO WEIGHT
        // -----------------------------
        val videoCodec = video.inputs
            .files()
            .flatMap { it.videoStreams }
            .firstOrNull()
            ?.codec

        val rawVideo = when (videoCodec) {
            is VideoCodec.Copy -> 0.10   // copy is cheap
            null -> 0.0
            else -> 0.60                // encode is heavy
        }

        // -----------------------------
        // AUDIO WEIGHTS
        // -----------------------------
        val audioConfigs = audio.flatMap { instr ->
            instr.inputs.files().flatMap { it.audioStreams }
        }

        val rawAudio = audioConfigs.map { config ->
            when (config.codec) {
                is AudioCodec.Copy -> 0.02   // cheap
                else -> 0.15                 // encode
            }
        }

        // -----------------------------
        // MERGE WEIGHT
        // -----------------------------
        val rawMerge = 0.05

        // -----------------------------
        // NORMALIZE
        // -----------------------------
        val total = rawVideo + rawAudio.sum() + rawMerge

        return Weights(
            video = if (total > 0) rawVideo / total else 0.0,
            audioPerTrack = rawAudio.map { if (total > 0) it / total else 0.0 },
            merge = if (total > 0) rawMerge / total else 0.0
        )
    }
}
