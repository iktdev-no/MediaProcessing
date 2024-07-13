package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class EncodeWorkPerformedEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.EventWorkEncodePerformed,
    override val data: EncodedData? = null,
    val message: String? = null
) : Event() {
}

data class EncodedData(
    val outputFile: String
)