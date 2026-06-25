package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.SingleTaskCratedEvent
import java.util.UUID

class StoreMediaInfoAndMetadataTaskCreatedEvent(
    taskId: UUID
): SingleTaskCratedEvent(taskId) {}