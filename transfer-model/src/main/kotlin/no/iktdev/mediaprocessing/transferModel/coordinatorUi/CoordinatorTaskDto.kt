package no.iktdev.mediaprocessing.transferModel.coordinatorUi

import java.time.Instant
import java.util.*

data class CoordinatorTaskDto(
    val id: Long,
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
    val logs: List<String> = emptyList(),
    val abandoned: Boolean,
    val availableOverrides: List<String> = emptyList(),
    val activeOverrides: List<String> = emptyList(),
)

