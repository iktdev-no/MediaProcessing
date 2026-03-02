package no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress

class FfmpegDecodedProgress(
    val time: String,
    val duration: String,
    val speed: String,
    val estimatedCompletionSeconds: Long = -1,
    val estimatedCompletion: String = "Unknown",
) {
}