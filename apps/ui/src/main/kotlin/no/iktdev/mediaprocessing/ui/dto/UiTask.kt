package no.iktdev.mediaprocessing.ui.dto

import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CoordinatorTaskDto
import java.time.Instant
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
    val lastCheckIn: Instant?,
    val persistedAt: Instant,
    val abandoned: Boolean,

    // Sanntidsfelter (kun fra SSE)
    val progress: Int? = null,
    val timeLeft: Double? = null,
    val speed: Double? = null,
    val elapsed: Double? = null,
) {
    companion object {
        fun from(c: CoordinatorTaskDto) = UiTask(
            id = c.id,
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
        )
    }
}