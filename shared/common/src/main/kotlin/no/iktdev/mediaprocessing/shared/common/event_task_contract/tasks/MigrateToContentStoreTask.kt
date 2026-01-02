package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

data class MigrateToContentStoreTask(
    val data: Data
): Task() {
    data class Data(
        val collection: String,
        val videoContent: SingleContent? = null,
        val subtitleContent: List<SingleSubtitle>? = null, // Both extracted and converted
        val coverContent: List<SingleContent>? = null
    ) {
        data class SingleContent(
            val cachedUri: String,
            val storeUri: String
        )
        data class SingleSubtitle(
            val language: String,
            val cachedUri: String,
            val storeUri: String
        )
    }
}