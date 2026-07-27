package no.iktdev.mediaprocessing.shared.common.dto.progress

class FfmpegDecodedProgress(
    val time: String,
    val duration: String,
    val speed: String,
    val estimatedCompletionSeconds: Long = -1,
    val estimatedCompletion: String = "Unknown",
) {
}