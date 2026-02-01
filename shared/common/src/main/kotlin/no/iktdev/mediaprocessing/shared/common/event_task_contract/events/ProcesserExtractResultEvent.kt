package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

class ProcesserExtractResultEvent(
    val data: ExtractResult? = null,
    status: TaskStatus,
    error: String? = null,
    logFile: String? = null,
) : TaskResultEvent(status, error, logFile) {
    data class ExtractResult(
        val language: String,
        val cachedOutputFile: String
    )
}