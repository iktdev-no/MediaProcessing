package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.model.MediaType

data class MetadataSearchTask(
    val data: SearchData
): Task() {
    data class SearchData(
        val searchTitles: List<String>,
        val collection: String,
        val mediaType: MediaType,
    )
}

