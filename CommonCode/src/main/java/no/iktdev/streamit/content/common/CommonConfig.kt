package no.iktdev.streamit.content.common

import java.io.File

object CommonConfig {
    var kafkaConsumerId: String = System.getenv("KAFKA_TOPIC") ?: "contentEvents"
    var incomingContent: File? = if (!System.getenv("DIRECTORY_CONTENT_INCOMING").isNullOrEmpty()) File(System.getenv("DIRECTORY_CONTENT_INCOMING")) else null

}