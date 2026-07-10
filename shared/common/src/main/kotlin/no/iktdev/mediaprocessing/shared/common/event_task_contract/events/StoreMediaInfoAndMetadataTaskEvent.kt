package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.SingleTaskCratedEvent
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import java.util.UUID

sealed interface StoreMediaInfoAndMetadataTask

class StoreMediaInfoAndMetadataTaskCreatedEvent(
    taskId: UUID
): SingleTaskCratedEvent(taskId), StoreMediaInfoAndMetadataTask {}

class StoreMediaInfoAndMetadataTaskResultEvent(
    status: TaskStatus,
    error: String? = null
) : TaskResultEvent(status, error), StoreMediaInfoAndMetadataTask{
}