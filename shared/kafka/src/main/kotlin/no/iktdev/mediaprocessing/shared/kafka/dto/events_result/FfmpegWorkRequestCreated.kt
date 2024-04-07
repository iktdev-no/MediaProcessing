package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(
    KafkaEvents.EventWorkEncodeCreated,
    KafkaEvents.EventWorkExtractCreated
)
data class FfmpegWorkRequestCreated(
    override val status: Status,
    val inputFile: String,
    val arguments: List<String>,
    val outFile: String,
    override val derivedFromEventId: String?
) : MessageDataWrapper(status, derivedFromEventId)