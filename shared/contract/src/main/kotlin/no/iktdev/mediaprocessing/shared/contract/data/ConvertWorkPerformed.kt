package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

class ConvertWorkPerformed(
    override val eventType: Events = Events.EventWorkConvertPerformed,
    override val metadata: EventMetadata,
    override val data: ConvertedData? = null,
    val message: String? = null
) : Event() {
}

data class ConvertedData(
    val outputFiles: List<String>
)
