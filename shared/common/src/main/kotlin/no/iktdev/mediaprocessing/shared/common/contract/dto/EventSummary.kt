package no.iktdev.mediaprocessing.shared.common.contract.dto

data class EventSummary(
    val operationsSummary: OperationsSummary,
    val inputFile: String,
    val inputFileChecksum: String,
    val outputFiles: OutputFiles
)


data class OperationsSummary(
    val requestedOperations: List<OperationEvents>,
    val completedOperations: List<OperationEvents>,
)

data class OutputFiles(
    val encoded: List<String>,
    val extracted: List<String>,
    val converted: List<String>
)