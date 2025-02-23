package no.iktdev.mediaprocessing.ui

import java.io.File

object UIEnv {
    val socketEncoder: String = if (System.getenv("EncoderWs").isNullOrBlank()) System.getenv("EncoderWs") else "ws://encoder:8080"
    val coordinatorUrl: String = System.getenv("Coordinator")?.takeIf { it.isNotBlank() } ?: "http://coordinator"
}