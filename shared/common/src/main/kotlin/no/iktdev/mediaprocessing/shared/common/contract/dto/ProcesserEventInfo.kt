package no.iktdev.mediaprocessing.shared.common.contract.dto

data class ProcesserEventInfo(
    val referenceId: String,
    val eventId: String,
    val status: WorkStatus = WorkStatus.Pending,
    val progress: ProcesserProgress? = null,
    val inputFile: String,
    val outputFiles: List<String>
)

data class ProcesserProgress(
    val progress: Int = -1,
    val speed: String? = null,
    val timeWorkedOn: String? = null,
    val timeLeft: String? = "Unknown", // HH mm
)