package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskCreatedEvent
import java.util.UUID

class ConvertTaskCreatedEvent(
    taskId: UUID
): TaskCreatedEvent(taskId = taskId) {
}