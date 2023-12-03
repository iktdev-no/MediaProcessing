package no.iktdev.mediaprocessing.shared.kafka

import no.iktdev.mediaprocessing.shared.SharedConfig
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultProducer
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status

class CoordinatorProducer(): DefaultProducer(SharedConfig.kafkaTopic) {
    fun sendMessage(referenceId: String, event: KafkaEvents, data: MessageDataWrapper) {
        super.sendMessage(event.event, Message(
            referenceId = referenceId,
            data = data
        ))
    }
    fun sendMessage(referenceId: String, event: KafkaEvents, eventId: String, data: MessageDataWrapper) {
        super.sendMessage(event.event, Message(
            referenceId = referenceId,
            eventId = eventId,
            data = data
        ))
    }
}