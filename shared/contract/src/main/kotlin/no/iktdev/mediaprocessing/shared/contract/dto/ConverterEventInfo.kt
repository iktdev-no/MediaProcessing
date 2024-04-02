package no.iktdev.mediaprocessing.shared.contract.dto

data class ConverterEventInfo(
    val status: WorkStatus = WorkStatus.Pending,
    val inputFile: String,
    val outputFiles: List<String> = emptyList()
)