package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus

data class ProcesserExtractResultEvent(
    val status: TaskStatus,
    val data: ExtractResult? = null
): Event() {
    data class ExtractResult(
        val language: String,
        val cachedOutputFile: String
    )
}