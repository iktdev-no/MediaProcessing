package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

class CoverDownloadSkippedEvent(
    error: String? = null,
    status: TaskStatus = TaskStatus.Failed
) : TaskResultEvent(status = status, error) {
    override fun newStatus(ns: TaskStatus): TaskResultEvent {
        return CoverDownloadSkippedEvent(error, ns).from(this)
    }
}

