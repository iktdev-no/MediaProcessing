package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EVENT_PROCESS_COMPLETED)
data class ProcessCompleted(override val status: Status) : MessageDataWrapper(status) {
}