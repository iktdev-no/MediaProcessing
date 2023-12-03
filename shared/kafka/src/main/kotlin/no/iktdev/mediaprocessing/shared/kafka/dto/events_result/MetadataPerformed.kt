package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EVENT_MEDIA_METADATA_SEARCH_PERFORMED)
data class MetadataPerformed(
    override val status: Status,
    override val message: String? = null,
    val data: pyMetadata? = null
    ) : MessageDataWrapper(status, message)

data class pyMetadata(
    val title: String,
    val altTitle: List<String> = emptyList(),
    val cover: String? = null,
    val type: String,
    val summary: String? = null,
    val genres: List<String> = emptyList()
)