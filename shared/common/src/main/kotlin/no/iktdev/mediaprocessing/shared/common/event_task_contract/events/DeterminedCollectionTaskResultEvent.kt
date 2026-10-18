package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models.CollectionReference

class DeterminedCollectionTaskResultEvent(
    status: TaskStatus,
    error: String? = null,
    val collectionReference: CollectionReference? = null,
): TaskResultEvent(status, error) {
    override fun newStatus(ns: TaskStatus): TaskResultEvent {
        return DeterminedCollectionTaskResultEvent(ns, error, collectionReference).from(this)
    }
}