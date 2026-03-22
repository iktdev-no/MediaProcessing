package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskCreatedEvent
import java.util.UUID

class ProcesserEncodeTaskCreatedEvent(
    taskId: UUID,
    val taskType: String
): TaskCreatedEvent(taskId = taskId) {}