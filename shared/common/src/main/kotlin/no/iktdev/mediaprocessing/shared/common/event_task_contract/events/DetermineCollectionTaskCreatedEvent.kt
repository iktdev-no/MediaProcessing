package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskCreatedEvent
import java.util.UUID

class DetermineCollectionTaskCreatedEvent(
    taskId: UUID
): TaskCreatedEvent(taskId = taskId) {
}