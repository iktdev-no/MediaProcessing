package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class ExtractWorkCreatedEvent(
    override val eventType: Events = Events.EventWorkExtractCreated,
    override val metadata: EventMetadata,
    override val data: ExtractArgumentData? = null
) : Event() {
}