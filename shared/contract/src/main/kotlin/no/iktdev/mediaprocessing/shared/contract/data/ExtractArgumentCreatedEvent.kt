package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class ExtractArgumentCreatedEvent(
    override val eventType: Events = Events.EventMediaParameterExtractCreated,
    override val metadata: EventMetadata,
    override val data: List<ExtractArgumentData>? = null

): Event()

data class ExtractArgumentData(
    val arguments: List<String>,
    val outputFile: String,
    val inputFile: String
)