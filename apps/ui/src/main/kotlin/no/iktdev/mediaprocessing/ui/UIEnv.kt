package no.iktdev.mediaprocessing.ui

import java.io.File

object UIEnv {
    var storedContent: File = if (!System.getenv("DIRECTORY_CONTENT_STORED").isNullOrBlank()) File(System.getenv("DIRECTORY_CONTENT_STORED")) else File("/src/output")
    val socketEncoder: String = if (System.getenv("EncoderWs").isNullOrBlank()) System.getenv("EncoderWs") else "ws://encoder:8080"

    val coordinatorUrl: String = if (System.getenv("Coordinator").isNullOrBlank()) System.getenv("Coordinator") else "http://coordinator"
}