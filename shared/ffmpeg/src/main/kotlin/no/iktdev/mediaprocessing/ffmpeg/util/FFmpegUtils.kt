package no.iktdev.mediaprocessing.ffmpeg.util

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import no.iktdev.mediaprocessing.ffmpeg.dsl.TranscodeDecision
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.AudioStreamConfig
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputConfig
import no.iktdev.mediaprocessing.ffmpeg.model.EncodeStrategy

enum class FfmpegCodecs(val ffmpegName: String) {
    hevc("libx265"),
    h264("libx264"),
    vp9("libvpx-vp9"),
    av1("libaom-av1"),
    vid("libxvid"),
    vvc("libvvc"),
    vp8("libvpx");

    fun getCodecs(): List<FfmpegCodecs> {
        return entries
    }
}


fun CodecNameToFfmpegCodec(name: String): FfmpegCodecs {
    val nz = name
        .lowercase()
        .replace(" ", "")
        .replace(".", "")
        .replace("-", "")

    return when (nz) {
        // HEVC / H.265
        "hevc", "hevec", "h265", "libx265", "x265",
        "hev1", "hvc1",
        "hevcvideotoolbox", "hevcnvenc", "hevcqsv" ->
            FfmpegCodecs.hevc

        // H.264
        "h264", "libx264", "x264" ->
            FfmpegCodecs.h264

        // VP9
        "vp9", "libvpxvp9" ->
            FfmpegCodecs.vp9

        // VP8
        "vp8", "libvpx" ->
            FfmpegCodecs.vp8

        // AV1
        "av1", "libaomav1" ->
            FfmpegCodecs.av1

        // MPEG4 / Xvid
        "mpeg4", "mp4", "libxvid", "xvid" ->
            FfmpegCodecs.vid

        // VVC / H.266
        "vvc", "h266", "libvvc" ->
            FfmpegCodecs.vvc

        else -> throw IllegalArgumentException("Unsupported codec: $name")
    }
}


fun VideoStream.durationSecondsOrNull(): Long? {
    // 1. duration_ts + time_base
    if (duration_ts != null && time_base.contains("/")) {
        val parts = time_base.split("/")
        if (parts.size == 2) {
            val num = parts[0].toLongOrNull()
            val den = parts[1].toLongOrNull()
            if (num != null && den != null && den > 0) {
                return (duration_ts * num / den)
            }
        }
    }

    // 2. parse duration string
    duration?.let { dur ->
        if (dur == "N/A") return null

        // HH:MM:SS.micro
        if (dur.contains(":")) {
            val parts = dur.split(":")
            if (parts.size == 3) {
                val hours = parts[0].toLongOrNull()
                val minutes = parts[1].toLongOrNull()
                val seconds = parts[2].toDoubleOrNull()

                if (hours != null && minutes != null && seconds != null) {
                    return ((hours * 3600) + (minutes * 60) + seconds).toLong()
                }
            }
        }

        // seconds.micro
        dur.toDoubleOrNull()?.let { return it.toLong() }
    }

    // 3. nothing worked
    return null
}


fun determineEncodeStrategy(
    decision: TranscodeDecision,
    stream: VideoStream
): EncodeStrategy {

    val durationSeconds = stream.durationSecondsOrNull()

    return when (decision) {

        TranscodeDecision.Copy,
        TranscodeDecision.Remux -> {
            // Copy/remux → alltid linear
            EncodeStrategy.Linear
        }

        TranscodeDecision.Reencode -> {
            // Hvis vi ikke vet varighet → straff med segmented
            if (durationSeconds == null) {
                return EncodeStrategy.Segmented
            }

            // Hvis kort → linear
            if (durationSeconds < 30 * 60) {
                EncodeStrategy.Linear
            } else {
                EncodeStrategy.Segmented
            }
        }
    }
}


fun FFmpegInstructions.resolveExpectedFullPath(store: IFile): IFile {
    val useFileName = this.output?.path ?: throw IllegalStateException("Output is missing on instruction")
    return store.using(useFileName)
}

fun FFmpegInstructions.getAudioMetadata(): AudioStreamConfig {
    val audioInstruction = this
    val allInputs = audioInstruction.inputs.files()

    // 1) Concat mode? → Ikke lov
    require(audioInstruction.inputs.concatInput == null) { "Concat not allowed in AudioEncodeRunner" }

    // 2) Normal mode → hent InputConfig
    val inputConfigs = allInputs.filterIsInstance<InputConfig>()
    if (inputConfigs.size != 1) {
        throw IllegalStateException("Audio instruction expects exactly 1 input file, but found ${inputConfigs.size}")
    }

    // 3) Hent audio streams
    val audioStreams = inputConfigs
        .flatMap { it.audioStreams }

    if (audioStreams.size != 1) {
        throw IllegalStateException("Audio instruction expects exactly 1 audio stream, but found ${audioStreams.size}")
    }

    return audioStreams.first()
}
