package no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress

class EncodeProgress(referenceId: String, taskId: String, progress: Int, val additionalInfo: FfmpegDecodedProgress): Progress(referenceId, taskId, progress) {
}