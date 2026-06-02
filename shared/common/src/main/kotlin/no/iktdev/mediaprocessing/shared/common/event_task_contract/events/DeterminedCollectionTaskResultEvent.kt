package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.eventi.models.store.TaskStatus

class DeterminedCollectionTaskResultEvent(
    status: TaskStatus,
    error: String? = null,
    val collection: String?
): TaskResultEvent(status, error) {

}