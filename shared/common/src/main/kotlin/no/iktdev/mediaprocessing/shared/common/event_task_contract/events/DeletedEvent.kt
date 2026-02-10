package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.DeleteEvent
import java.util.UUID

class DeletedEvent(deletedEventId: UUID) : DeleteEvent(deletedEventId) {
}