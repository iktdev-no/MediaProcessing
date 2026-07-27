package no.iktdev.mediaprocessing.shared.common.dto

data class DiskInfo(
    val mount: String,
    val device: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val usedPercent: Double
)