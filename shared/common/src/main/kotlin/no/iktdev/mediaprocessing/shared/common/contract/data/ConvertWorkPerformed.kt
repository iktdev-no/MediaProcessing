package no.iktdev.mediaprocessing.shared.common.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.common.contract.Events

class ConvertWorkPerformed(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.EventWorkConvertPerformed,
    override val data: ConvertedData? = null,
    val message: String? = null
) : Event() {
}

data class ConvertedData(
    val outputFiles: List<String>
)
