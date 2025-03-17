package no.iktdev.mediaprocessing.ui

import java.io.File

object UIEnv {
    val socketEncoder: String = System.getenv("EncoderWs")?.takeIf { it.isNotBlank() } ?: "ws://encoder:8080"
    val coordinatorUrl: String = System.getenv("Coordinator")?.takeIf { it.isNotBlank() } ?: "http://coordinator"
    val wsAllowedOrigins: String = System.getenv("AllowedOriginsWebsocket")?.takeIf { it.isNotBlank() } ?: ""
}