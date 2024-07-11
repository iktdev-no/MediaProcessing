package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class ConvertWorkCreatedEvent(
    override val eventType: Events = Events.EventWorkConvertCreated,
    override val metadata: EventMetadata,
    override val data: ConvertData? = null
) : Event() {
}

data class ConvertData(
    val inputFile: String,
    val outputDirectory: String,
    val outputFileName: String,
    val formats: List<String> = emptyList(),
    val allowOverwrite: Boolean
)