package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

data class MetadataSearchTask(
    val data: SearchData
): Task() {
    data class SearchData(
        val searchTitles: List<String>,
        val collection: String,
    )
}

