package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus

data class ConvertTaskResultEvent(
    val data: ConvertedData?,
    val status: TaskStatus,
): Event() {
    data class ConvertedData(
        val language: String,
        val baseName: String,
        val outputFiles: List<String>
    )
}

