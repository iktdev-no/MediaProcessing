package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

class CoverDownloadResultEvent(
    val data: CoverDownloadedData? = null,
    status: TaskStatus,
    error: String? = null
) : TaskResultEvent(status, error){
    data class CoverDownloadedData(
        val source: String,
        val outputFile: String
    )
}

