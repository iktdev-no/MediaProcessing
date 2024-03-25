package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(
    KafkaEvents.EVENT_WORK_ENCODE_CREATED,
    KafkaEvents.EVENT_WORK_EXTRACT_CREATED
)
data class FfmpegWorkRequestCreated(
    override val status: Status,
    val derivedFromEventId: String,
    val inputFile: String,
    val arguments: List<String>,
    val outFile: String
): MessageDataWrapper(status)