package no.iktdev.mediaprocessing.shared.common.event_task_contract.events.delete

import no.iktdev.eventi.models.DeleteEvent
import java.util.*

class DeletedTaskResultEvent(deletedEventId: UUID): DeleteEvent(deletedEventId = deletedEventId) {
}