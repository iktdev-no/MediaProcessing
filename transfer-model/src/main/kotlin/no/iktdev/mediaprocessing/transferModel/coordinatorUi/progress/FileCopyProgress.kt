package no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress

class FileCopyProgress(
    referenceId: String, taskId: String, progress: Int, val source: String, val destination: String,
) : Progress(referenceId, taskId, progress) {
}