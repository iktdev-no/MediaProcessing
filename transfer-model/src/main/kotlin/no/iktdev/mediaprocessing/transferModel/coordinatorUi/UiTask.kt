package no.iktdev.mediaprocessing.transferModel.coordinatorUi

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
) {
    companion object {
        fun from(c: CoordinatorTaskDto) = UiTask(
            referenceId = c.referenceId,
            status = c.status,
            taskId = c.taskId,
            task = c.task,
            data = c.data,
            claimed = c.claimed,
            claimedBy = c.claimedBy,
            consumed = c.consumed,
            lastCheckIn = c.lastCheckIn,
            persistedAt = c.persistedAt,
            abandoned = c.abandoned,
            logFiles = c.logs,
            availableOverrides = c.availableOverrides,
            activeOverrides = c.activeOverrides,
        )
    }
}