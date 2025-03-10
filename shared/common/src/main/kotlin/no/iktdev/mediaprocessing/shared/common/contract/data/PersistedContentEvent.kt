package no.iktdev.mediaprocessing.shared.common.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.common.contract.Events

data class PersistedContentEvent(
    override val eventType: Events = Events.PersistContent,
    override val metadata: EventMetadata,
    override val data: PersistedContent?

): Event() {}

data class PersistedContent(
    val cover: PersistedItem?,
    val video: PersistedItem?,
    val subtitles: List<PersistedItem>
)

data class PersistedItem(
    val cacheSourceFileName: String,
    val storeDestinationFileName: String
)

