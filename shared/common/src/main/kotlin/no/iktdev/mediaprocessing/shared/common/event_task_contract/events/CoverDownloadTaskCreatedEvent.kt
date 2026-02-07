package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.MultiTaskCreatedEvent
import java.util.UUID

class CoverDownloadTaskCreatedEvent(
    taskIds: List<UUID>
): MultiTaskCreatedEvent(taskIds = taskIds) {
}