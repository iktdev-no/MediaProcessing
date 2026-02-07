package no.iktdev.mediaprocessing.ffmpeg.decoder


data class FfmpegDecodedProgress(
    val progress: Int = -1,
    val time: String,
    val duration: String,
    val speed: String,
    val estimatedCompletionSeconds: Long = -1,
    val estimatedCompletion: String = "Unknown",
) {
}

