package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus

data class ProcesserEncodeResultEvent(
    val data: EncodeResult? = null,
    val status: TaskStatus,
): Event() {
    data class EncodeResult(
        val cachedOutputFile: String? = null
    )
}