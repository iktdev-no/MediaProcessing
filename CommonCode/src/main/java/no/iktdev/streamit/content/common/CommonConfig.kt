package no.iktdev.streamit.content.common

import java.io.File

object CommonConfig {
    var kafkaTopic: String = System.getenv("KAFKA_TOPIC") ?: "contentEvents"
    var incomingContent: File = if (!System.getenv("DIRECTORY_CONTENT_INCOMING").isNullOrBlank()) File(System.getenv("DIRECTORY_CONTENT_INCOMING")) else File("/src/input")
    val outgoingContent: File = if (!System.getenv("DIRECTORY_CONTENT_OUTGOING").isNullOrBlank()) File(System.getenv("DIRECTORY_CONTENT_OUTGOING")) else File("/src/output")
}