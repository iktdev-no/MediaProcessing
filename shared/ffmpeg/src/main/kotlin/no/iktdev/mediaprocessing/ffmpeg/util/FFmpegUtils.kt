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
    return when (name.lowercase()) {
        "hevc", "hevec", "h265", "h.265", "libx265" -> FfmpegCodecs.hevc
        "h.264", "h264", "libx264" -> FfmpegCodecs.h264
        "vp9", "vp-9", "libvpx-vp9" -> FfmpegCodecs.vp9
        "av1", "libaom-av1" -> FfmpegCodecs.av1
        "mpeg4", "mp4", "libxvid" -> FfmpegCodecs.vid
        "vvc", "h.266", "libvvc" -> FfmpegCodecs.vvc
        "vp8", "libvpx" -> FfmpegCodecs.vp8
        else -> throw IllegalArgumentException("Unsupported codec: $name")
    }
}


fun getBestEncodeStrategy(codec: VideoCodec, stream: VideoStream): EncodeStrategy {
    val transcodeDecision = codec.determineTranscodeDecision(stream)
    return when (transcodeDecision) {
        TranscodeDecision.Copy -> EncodeStrategy.Linear
        TranscodeDecision.Remux -> EncodeStrategy.Linear
        TranscodeDecision.Reencode -> EncodeStrategy.Segmented
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
