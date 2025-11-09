package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.DeleteEvent
import no.iktdev.eventi.models.Event

data class FileAddedEvent(
    val data: FileInfo
): Event() {
}

data class FileReadyEvent(
    val data: FileInfo
): Event() {}

class FileRemovedEvent(
    val data: FileInfo
): DeleteEvent() {
}

data class FileInfo(
    val fileName: String,
    val fileUri: String,
)