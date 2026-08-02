package no.iktdev.mediaprocessing.ui.models.contract

data class DiskInfo(
    val mount: String,
    val device: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val usedPercent: Double
)