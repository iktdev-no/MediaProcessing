package no.iktdev.mediaprocessing.ui.dto

import java.time.LocalDateTime
import java.util.*

data class UiTask(
    val id: Long,
    val referenceId: UUID,
    val status: String,
    val taskId: UUID,
    val task: String,
    val data: String,
    val claimed: Boolean,
    val claimedBy: String?,
    val consumed: Boolean,
    val lastCheckIn: LocalDateTime?,
    val persistedAt: LocalDateTime,
    val abandoned: Boolean,

    // Sanntidsfelter (kun fra SSE)
    val progress: Int? = null,
    val timeLeft: Double? = null,
    val speed: Double? = null,
    val elapsed: Double? = null,
)