package no.iktdev.mediaprocessing.shared.common.event_task_contract.events.delete

import no.iktdev.eventi.models.DeleteEvent
import java.util.UUID

class DeletedEvent(deletedEventId: UUID) : DeleteEvent(deletedEventId) {
}