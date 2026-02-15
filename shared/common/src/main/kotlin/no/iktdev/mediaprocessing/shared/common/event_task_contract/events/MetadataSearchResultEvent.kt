package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Metadata
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import java.util.*

class MetadataSearchResultEvent(
    val results: List<SearchResult> = emptyList(),
    val recommended: SearchResult? = null,
    status: TaskStatus,
    error: String? = null
) : TaskResultEvent(status, error) {

    data class SearchResult(
        val searchTitles: List<String>,
        val similarity: Int,
        val prefix: Int,
        val keywordScore: Double,
        val typeScore: Double,
        val completenessScore: Double,
        val sourceScore: Double,
        val totalScore: Double,
        val metadata: MetadataResult
    ) {

        data class MetadataResult(
            val source: String,
            val title: String,
            val alternateTitles: List<String> = emptyList(),
            val cover: String?,
            val bannerImage: String? = null,
            val type: MediaType,
            val summary: List<Summary>,
            val genres: List<String>
        ) {
            data class Summary(
                val language: String,
                val description: String
            )
        }
    }

    fun setFailed(derivedIds: List<UUID>) {
        assert(status == TaskStatus.Failed)
        val metadata = Metadata().apply {
            this.derivedFromEventId(derivedIds.toSet())
        }
        this.metadata = metadata
    }
}
