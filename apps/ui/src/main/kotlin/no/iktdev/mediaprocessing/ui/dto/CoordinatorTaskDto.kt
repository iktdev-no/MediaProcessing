package no.iktdev.mediaprocessing.ui.dto

import java.time.LocalDateTime
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
    val lastCheckIn: LocalDateTime?,
    val persistedAt: LocalDateTime,
    val abandoned: Boolean,
) {
}

fun CoordinatorTaskDto.toUiTask() = UiTask(
    id = id,
    referenceId = referenceId,
    status = status,
    taskId = taskId,
    task = task,
    data = data,
    claimed = claimed,
    claimedBy = claimedBy,
    consumed = consumed,
    lastCheckIn = lastCheckIn,
    persistedAt = persistedAt,
    abandoned = abandoned,
)
