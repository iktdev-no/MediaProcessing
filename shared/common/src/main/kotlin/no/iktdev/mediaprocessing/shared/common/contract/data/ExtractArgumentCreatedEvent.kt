package no.iktdev.mediaprocessing.shared.common.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.dto.tasks.TaskData

data class ExtractArgumentCreatedEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.EventMediaParameterExtractCreated,
    override val data: List<ExtractArgumentData>? = null

): Event()

data class ExtractArgumentData(
    val arguments: List<String>,
    val outputFile: String,
    override val inputFile: String
): TaskData()