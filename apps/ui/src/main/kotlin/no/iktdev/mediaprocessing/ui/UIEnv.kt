package no.iktdev.mediaprocessing.ui

import java.io.File

object UIEnv {
    val socketEncoder: String = if (System.getenv("EncoderWs").isNullOrBlank()) System.getenv("EncoderWs") else "ws://encoder:8080"
    val coordinatorUrl: String = if (System.getenv("Coordinator").isNullOrBlank()) System.getenv("Coordinator") else "http://coordinator"
}