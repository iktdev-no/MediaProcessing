package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EVENT_WORK_ENCODE_CREATED)
data class MediaEncodeInfo(
    override val status: Status,
    val arguments: List<String>
) :
    MessageDataWrapper(status)