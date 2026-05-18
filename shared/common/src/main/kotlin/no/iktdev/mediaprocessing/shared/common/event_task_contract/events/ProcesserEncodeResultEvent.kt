package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

class ProcesserEncodeResultEvent(
    val data: EncodeResult? = null,
    logFile: String? = null,
    status: TaskStatus,
    error: String? = null
) : TaskResultEvent(status, error, logFile) {
    data class EncodeResult(
        val cachedOutputFile: String? = null,
        val cachedSegmentFiles: List<String>? = null
    )
}