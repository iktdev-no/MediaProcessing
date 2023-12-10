package no.iktdev.mediaprocessing.shared.kafka.core

import java.util.UUID

class KafkaEnv {
    companion object {
        val servers: String = System.getenv("KAFKA_BOOTSTRAP_SERVER") ?: "127.0.0.1:9092"
        var consumerId: String = System.getenv("KAFKA_CONSUMER_ID") ?: "LibGenerated-${UUID.randomUUID()}"
        var enabled: Boolean = System.getenv("KAFKA_ENABLED").toBoolean()
        val loadMessages: String = System.getenv("KAFKA_MESSAGES_USE") ?: "earliest"

        var kafkaTopic: String = System.getenv("KAFKA_TOPIC") ?: "contentEvents"

    }
}