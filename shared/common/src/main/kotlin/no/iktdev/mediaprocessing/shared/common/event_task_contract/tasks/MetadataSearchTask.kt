package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

data class MetadataSearchTask(
    val data: MetadataSearchData
): Task() {}

data class MetadataSearchData(
    val searchString: String,
    val maxResults: Int = 10,
    val offset: Int = 0,
)