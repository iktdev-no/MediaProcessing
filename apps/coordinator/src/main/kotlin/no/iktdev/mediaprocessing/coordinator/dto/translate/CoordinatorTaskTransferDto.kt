package no.iktdev.mediaprocessing.coordinator.dto.translate

import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.mediaprocessing.shared.common.rules.TaskLifecycleRules
import java.time.Instant
import java.util.*

data class CoordinatorTaskTransferDto(
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
) {
}

fun PersistedTask.toCoordinatorTransferDto(): CoordinatorTaskTransferDto {
    return CoordinatorTaskTransferDto(
        id = id,
        referenceId = referenceId,
        status = status.name,
        taskId = taskId,
        task = task,
        data = data,
        claimed = claimed,
        claimedBy = claimedBy,
        consumed = consumed,
        lastCheckIn = lastCheckIn,
        persistedAt = persistedAt,
        abandoned = TaskLifecycleRules.isAbandoned(consumed, lastCheckIn)
    )
}
