package no.iktdev.streamit.content.common

import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.consumers.DefaultConsumer
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
import no.iktdev.streamit.library.kafka.producer.DefaultProducer

abstract class DefaultKafkaReader(val subId: String) {
    val messageProducer = DefaultProducer(CommonConfig.kafkaTopic)
    val defaultConsumer = DefaultConsumer(subId = subId)

    open fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return emptyMap()
    }

    fun produceErrorMessage(event: KafkaEvents, baseMessage: Message, reason: String) {
        val message = Message(
            referenceId = baseMessage.referenceId,
            Status(statusType = StatusType.ERROR, message = reason)
        )
        messageProducer.sendMessage(event.event, message)
    }

    fun produceErrorMessage(event: KafkaEvents, referenceId: String, reason: String) {
        val message = Message(
            referenceId = referenceId,
            Status(statusType = StatusType.ERROR, message = reason)
        )
        messageProducer.sendMessage(event.event, message)
    }

    fun produceMessage(event: KafkaEvents, baseMessage: Message, data: Any?) {
        val message = Message(
            referenceId = baseMessage.referenceId,
            Status(statusType = if (data != null) StatusType.SUCCESS else StatusType.IGNORED),
            data = data
        )
        messageProducer.sendMessage(event.event, message)
    }
}