package no.iktdev.mediaprocessing.ffmpeg.util

import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.TranscodeDecision
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.plan.BaseMediaPlan
import no.iktdev.mediaprocessing.ffmpeg.dsl.plan.LinearMediaPlan
import no.iktdev.mediaprocessing.ffmpeg.dsl.plan.SegmentedMediaPlan
import no.iktdev.mediaprocessing.ffmpeg.model.AudioClamp
import no.iktdev.mediaprocessing.ffmpeg.model.AudioTarget
import no.iktdev.mediaprocessing.ffmpeg.model.EncodeStrategy
import no.iktdev.mediaprocessing.ffmpeg.model.VideoTarget

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

fun getMediaPlanner(strategy: EncodeStrategy, videoTarget: VideoTarget, audioTargets: List<AudioTarget>): BaseMediaPlan {
    return when (strategy) {
        EncodeStrategy.Segmented -> SegmentedMediaPlan(videoTrack = videoTarget, audioTracks = audioTargets)
        EncodeStrategy.Linear -> LinearMediaPlan(videoTrack = videoTarget, audioTracks = audioTargets)
    }
}