package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class EncodeArgumentCreatedEvent(
    override val eventType: Events = Events.EventMediaParameterEncodeCreated,
    override val metadata: EventMetadata,
    override val data: EncodeArgumentData? = null
) : Event() {
}

data class EncodeArgumentData(
    val arguments: List<String>,
    val outputFile: String,
    val inputFile: String
)