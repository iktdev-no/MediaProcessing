package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

class StoreMediaInfoAndMetadataTaskResultEvent(
    status: TaskStatus,
    error: String? = null
) : TaskResultEvent(status, error) {
    override fun newStatus(ns: TaskStatus): TaskResultEvent {
        return StoreMediaInfoAndMetadataTaskResultEvent(ns, error).from(this)
    }
}