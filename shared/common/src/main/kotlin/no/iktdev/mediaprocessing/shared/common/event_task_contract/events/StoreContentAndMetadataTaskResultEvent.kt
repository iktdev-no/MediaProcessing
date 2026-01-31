package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

data class StoreContentAndMetadataTaskResultEvent(
    override val status: TaskStatus,
    override val error: String? = null
) : TaskResultEvent(status, error){
}