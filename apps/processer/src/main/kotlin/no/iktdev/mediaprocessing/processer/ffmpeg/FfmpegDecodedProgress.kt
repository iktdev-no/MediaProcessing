package no.iktdev.mediaprocessing.processer.ffmpeg

data class FfmpegDecodedProgress(
    val progress: Int = -1,
    val time: String,
    val duration: String,
    val speed: String,
    val estimatedCompletionSeconds: Long = -1,
    val estimatedCompletion: String = "Unknown",
)

data class ECT(val day: Int = 0, val hour: Int = 0, val minute: Int = 0, val second: Int = 0)