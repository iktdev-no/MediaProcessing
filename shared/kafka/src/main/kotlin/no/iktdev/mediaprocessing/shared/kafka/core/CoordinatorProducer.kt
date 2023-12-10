package no.iktdev.mediaprocessing.shared.kafka.core

import com.google.gson.Gson
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
open class CoordinatorProducer() {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    fun sendMessage(referenceId: String, event: KafkaEvents, data: MessageDataWrapper) {
        send( KafkaEnv.kafkaTopic,
            event.event, Message(
                referenceId = referenceId,
                data = data
            )
        )
    }

    fun sendMessage(referenceId: String, event: KafkaEvents, eventId: String, data: MessageDataWrapper) {
        send( KafkaEnv.kafkaTopic,
            event.event, Message(
                referenceId = referenceId,
                eventId = eventId,
                data = data
            )
        )
    }

    open fun send(topic: String, key: String, message: Message<MessageDataWrapper>) {
        val serializedMessage = serializeMessage(message)
        try {
            kafkaTemplate.send(ProducerRecord(topic, key, serializedMessage))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serializeMessage(message: Message<MessageDataWrapper>): String {
        val gson = Gson()
        return gson.toJson(message)
    }
}