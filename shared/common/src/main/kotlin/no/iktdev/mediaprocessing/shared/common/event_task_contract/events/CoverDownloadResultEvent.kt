package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus

data class CoverDownloadResultEvent(
    val data: CoverDownloadedData? = null,
    val status: TaskStatus
): Event() {
    data class CoverDownloadedData(
        val source: String,
        val outputFile: String
    )
}

