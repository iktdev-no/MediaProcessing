package no.iktdev.mediaprocessing.shared.common.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.dto.tasks.TaskData

data class EncodeArgumentCreatedEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.ParameterEncodeCreated,
    override val data: EncodeArgumentData? = null
) : Event() {

}

data class EncodeArgumentData(
    val arguments: List<String>,
    val outputFileName: String,
    override val inputFile: String
): TaskData()