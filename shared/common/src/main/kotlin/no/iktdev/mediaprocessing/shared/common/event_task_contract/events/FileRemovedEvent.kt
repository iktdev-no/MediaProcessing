package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.DeleteEvent
import no.iktdev.mediaprocessing.shared.common.model.FileInfo

class FileRemovedEvent(
    val data: FileInfo
): DeleteEvent() {
}