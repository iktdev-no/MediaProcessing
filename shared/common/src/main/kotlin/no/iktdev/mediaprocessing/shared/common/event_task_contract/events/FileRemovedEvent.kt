package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.DeleteEvent
import no.iktdev.mediaprocessing.shared.common.model.FileInfo
import java.util.*

class FileRemovedEvent(
    val addedEventId: UUID,
    val data: FileInfo
): DeleteEvent(addedEventId) {
}