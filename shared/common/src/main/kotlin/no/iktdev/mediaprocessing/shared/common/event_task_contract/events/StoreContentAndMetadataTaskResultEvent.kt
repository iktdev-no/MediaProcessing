package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus

data class StoreContentAndMetadataTaskResultEvent(
    val taskStatus: TaskStatus,
) : Event() {
}