package no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer

import no.iktdev.eventi.models.Metadata
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events_super.TransferredBaseResultEvent

class CoverTransferredResultEvent(
    val fileUri: String?,
    val collection: String,
    status: TaskStatus,
    error: String?): TransferredBaseResultEvent(status, error) {

    override fun newStatus(ns: TaskStatus): TaskResultEvent {
        val useResult = if (ns in listOf(TaskStatus.Completed)) fileUri else null
        return CoverTransferredResultEvent(useResult, collection, ns, error).from(this)
    }
}