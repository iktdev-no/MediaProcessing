package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.dto.SubtitleFormats
import no.iktdev.mediaprocessing.shared.contract.dto.tasks.TaskData

data class ConvertWorkCreatedEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.EventWorkConvertCreated,
    override val data: ConvertData? = null
) : Event() {
}

data class ConvertData(
    override val inputFile: String,
    val outputDirectory: String,
    val outputFileName: String,
    val formats: List<SubtitleFormats> = emptyList(),
    val allowOverwrite: Boolean
): TaskData()