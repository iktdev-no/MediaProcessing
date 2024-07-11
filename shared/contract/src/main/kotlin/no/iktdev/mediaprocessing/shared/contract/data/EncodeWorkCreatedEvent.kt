package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class EncodeWorkCreatedEvent(
    override val eventType: Events = Events.EventWorkEncodeCreated,
    override val metadata: EventMetadata,
    override val data: EncodeArgumentData? = null
) : Event()