package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.SingleTaskCreatedEvent
import java.util.UUID

class ProcesserEncodeTaskCreatedEvent(
    taskId: UUID,
    val taskType: String
): SingleTaskCreatedEvent(taskId = taskId) {}