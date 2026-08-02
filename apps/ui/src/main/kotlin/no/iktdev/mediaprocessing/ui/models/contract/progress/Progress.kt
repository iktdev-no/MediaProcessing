package no.iktdev.mediaprocessing.ui.models.contract.progress

import java.util.UUID


sealed class Progress(val referenceId: UUID, val taskId: UUID, val progress: Int)

class EncodeProgress(referenceId: UUID, taskId: UUID, progress: Int, val additionalInfo: FfmpegDecodedProgress? = null): Progress(referenceId, taskId, progress) {
}

class FileCopyProgress(referenceId: UUID, taskId: UUID, progress: Int, val source: String, val destination: String, val message: String = "") : Progress(referenceId, taskId, progress)


class SimpleProgress(referenceId: UUID, taskId: UUID, progress: Int): Progress(referenceId, taskId, progress) {}


class FfmpegDecodedProgress(
    val time: String,
    val duration: String,
    val speed: String,
    val estimatedCompletionSeconds: Long = -1,
    val estimatedCompletion: String = "Unknown",
) {
}