package no.iktdev.mediaprocessing.transferModel.coordinatorUi

data class ProgressUpdate(val referenceId: String, val taskId: String, val envelope: ProgressEnvelope)

data class ProgressEnvelope(
    val type: String,
    val data: String
)