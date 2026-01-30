package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.DeleteEvent
import java.util.*

class DeletedTaskResultEvent(deletedEventId: UUID): DeleteEvent(deletedEventId = deletedEventId) {
}