package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EventMediaMetadataSearchPerformed)
data class MetadataPerformed(
    override val status: Status,
    override val message: String? = null,
    val data: pyMetadata? = null,
    override val derivedFromEventId: String?
) : MessageDataWrapper(status, derivedFromEventId)

data class pyMetadata(
    val title: String,
    val altTitle: List<String> = emptyList(),
    val cover: String? = null,
    val type: String,
    val summary: List<pySummary> = emptyList(),
    val genres: List<String> = emptyList()
)

data class pySummary(
    val summary: String?,
    val language: String = "eng"
)