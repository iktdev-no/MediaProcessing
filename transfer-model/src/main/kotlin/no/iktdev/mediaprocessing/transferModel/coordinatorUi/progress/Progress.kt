package no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress

sealed class Progress(val referenceId: String, val taskId: String, val progress: Int) {
}

class SimpleProgress(referenceId: String, taskId: String, progress: Int): Progress(referenceId, taskId, progress) {}
