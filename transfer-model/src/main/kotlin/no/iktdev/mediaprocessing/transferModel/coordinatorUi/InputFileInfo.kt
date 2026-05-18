package no.iktdev.mediaprocessing.transferModel.coordinatorUi

data class InputFileInfo(
    val usedInReferences: List<String>,
    val fileUri: String,
    val fileName: String,
    val preserved: Boolean = false
)
