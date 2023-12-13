package no.iktdev.mediaprocessing.shared.kafka.core

import java.util.UUID

class KafkaEnv {
    companion object {
        val servers: String = System.getenv("KAFKA_BOOTSTRAP_SERVER") ?: "127.0.0.1:9092"
        var consumerId: String = System.getenv("KAFKA_CONSUMER_ID") ?: "LibGenerated-${UUID.randomUUID()}"
        val loadMessages: String = System.getenv("KAFKA_MESSAGES_USE") ?: "earliest"

        var kafkaTopic: String = System.getenv("KAFKA_TOPIC") ?: "contentEvents"
        val metadataTimeoutMinutes: Int = System.getenv("METADATA_TIMEOUT")?.toIntOrNull() ?: 10

        val heartbeatIntervalMilliseconds: Int = System.getenv("KAFKA_HEARTBEAT_INTERVAL_MS")?.toIntOrNull() ?: 2000
        val sessionTimeOutMilliseconds: Int = System.getenv("KAFKA_SESSION_INACTIVITY_MS")?.toIntOrNull() ?: (listOf(
            metadataTimeoutMinutes,
            heartbeatIntervalMilliseconds
        ).max() * 60)
    }
}