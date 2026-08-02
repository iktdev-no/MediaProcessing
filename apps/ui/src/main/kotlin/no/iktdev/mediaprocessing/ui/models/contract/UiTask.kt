package no.iktdev.mediaprocessing.ui.models.contract

import java.time.Instant
import java.util.*

data class UiTask(
    val referenceId: UUID,
    val status: String,
    val taskId: UUID,
    val task: String,
    val data: String,
    val claimed: Boolean,
    val claimedBy: String?,
    val consumed: Boolean,
    val lastCheckIn: Instant?,
    val persistedAt: Instant,
    val abandoned: Boolean,
    val logFiles: List<String> = emptyList(),
    val availableOverrides: List<String> = emptyList(),
    val activeOverrides: List<String> = emptyList(),

    // Sanntidsfelter (kun fra SSE)
    val progress: Int? = null,
    val timeLeft: Double? = null,
    val speed: Double? = null,
    val elapsed: Double? = null,
    val logs: List<String> = emptyList(),
)