package no.iktdev.mediaprocessing.shared.common.dto.progress

class FileCopyProgress(
    referenceId: String, taskId: String, progress: Int, val source: String, val destination: String,
) : Progress(referenceId, taskId, progress) {
}