package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class MediaCoverInfoReceivedEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.EventMediaReadOutCover,
    override val data: CoverDetails? = null
) : Event() {
}

data class CoverDetails(
    val url: String,
    val outDir: String,
    val outFileBaseName: String,
)