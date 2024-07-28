package no.iktdev.mediaprocessing.shared.common.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.common.contract.Events

data class ExtractWorkCreatedEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.EventWorkExtractCreated,
    override val data: ExtractArgumentData? = null
) : Event() {
}