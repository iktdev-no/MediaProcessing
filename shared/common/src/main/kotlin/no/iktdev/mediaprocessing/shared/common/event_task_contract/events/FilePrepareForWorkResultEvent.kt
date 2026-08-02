package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

class FilePrepareForWorkResultEvent(
    status: TaskStatus,
    val file: String? = null,
    error: String? = null,
): TaskResultEvent(status = status, error = error) {
    override fun newStatus(ns: TaskStatus): TaskResultEvent {
        return FilePrepareForWorkResultEvent(ns, file, error).from(this)
    }
}