package no.iktdev.mediaprocessing.shared.common.dto.progress

class EncodeProgress(referenceId: String, taskId: String, progress: Int, val additionalInfo: FfmpegDecodedProgress): Progress(referenceId, taskId, progress) {
}