package no.iktdev.mediaprocessing.shared.kafka.core

import com.google.gson.Gson
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

open class DefaultProducer(val topic: String) {
    private var kafkaTemplate:  KafkaTemplate<String, String>? = null


    open fun createKafkaTemplate(): KafkaTemplate<String, String> {
        val producerFactory: ProducerFactory<String, String>

        val config: MutableMap<String, Any> = HashMap()
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = KafkaEnv.servers
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java

        producerFactory = DefaultKafkaProducerFactory(config)
        return KafkaTemplate(producerFactory)
    }

    open fun usingKafkaTemplate(): KafkaTemplate<String, String> {
        return kafkaTemplate ?: createKafkaTemplate().also { kafkaTemplate = it }
    }

    open fun sendMessage(key: String, message: Message<MessageDataWrapper>) {
        val kafkaTemplate = usingKafkaTemplate()
        val serializedMessage = serializeMessage(message)
        kafkaTemplate.send(ProducerRecord(topic, key, serializedMessage))
    }

    private fun serializeMessage(message: Message<MessageDataWrapper>): String {
        val gson = Gson()
        return gson.toJson(message)
    }
}
