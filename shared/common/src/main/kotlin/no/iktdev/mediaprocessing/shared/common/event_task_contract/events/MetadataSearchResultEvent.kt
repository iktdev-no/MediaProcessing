package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Metadata
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import java.util.UUID

data class MetadataSearchResultEvent(
    val results: List<SearchResult> = emptyList(),
    val recommended: SearchResult? = null,
    val status: TaskStatus
): Event() {
    data class SearchResult(
        val simpleScore: Int,
        val prefixScore: Int,
        val advancedScore: Int,
        val sourceWeight: Float,
        val data: MetadataResult
    ) {

        data class MetadataResult(
            val source: String,
            val title: String,
            val alternateTitles: List<String> = emptyList(),
            val cover: String,
            val bannerImage: String? = null,
            val type: MediaType,
            val summary: List<Summary>,
            val genres: List<String>
        ) {
            data class Summary(val language: String, val description: String)
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