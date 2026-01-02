package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import java.util.UUID

data class MetadataSearchTaskCreatedEvent(
    val taskId: UUID
): Event() {
}