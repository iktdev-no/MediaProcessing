package no.iktdev.mediaprocessing.shared.common.dto

data class InputFileInfo(
    val usedInReferences: List<String>,
    val fileUri: String,
    val fileName: String,
    val preserved: Boolean = false
)