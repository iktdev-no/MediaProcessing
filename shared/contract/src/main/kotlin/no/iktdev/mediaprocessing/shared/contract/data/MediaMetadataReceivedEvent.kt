package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class MediaMetadataReceivedEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.EventMediaMetadataSearchPerformed,
    override val data: pyMetadata? = null,
    ): Event() {
}

data class pyMetadata(
    val title: String,
    val altTitle: List<String> = emptyList(),
    val cover: String? = null,
    val type: String,
    val summary: List<pySummary> = emptyList(),
    val genres: List<String> = emptyList()
)

data class pySummary(
    val summary: String?,
    val language: String = "eng"
)