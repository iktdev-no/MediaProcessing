package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class ExtractWorkPerformedEvent(
    override val eventType: Events = Events.EventWorkExtractPerformed,
    override val metadata: EventMetadata,
    override val data: ExtractedData? = null,
    val message: String? = null
) : Event() {
}

data class ExtractedData(
    val outputFile: String
)