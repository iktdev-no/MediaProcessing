package no.iktdev.mediaprocessing.shared.common.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.reader.SummaryInfo

data class MediaProcessCompletedEvent(
    override val metadata: EventMetadata,
    override val data: CompletedData?,
    override val eventType: Events = Events.ProcessCompleted
): Event()

data class CompletedData(
    val eventIdsCollected: List<String>,
    val metadataStored: MetadataStored
)

data class MetadataStored(
    val title: String,
    val titles: List<String>,
    val type: String,
    val cover: String? = null,
    val collection: String,
    val summary: List<SummaryInfo> = emptyList(),
    val foundTitles: List<String>,
    val genres: List<String>,
    val genreIds: String? = null
)