package no.iktdev.streamit.content.ui

import java.io.File

class UIEnv {
    companion object {
        var incomingContent: File = if (!System.getenv("DIRECTORY_CONTENT_INCOMING").isNullOrBlank()) File(System.getenv("DIRECTORY_CONTENT_INCOMING")) else File("/src/input")
        val socketEncoder: String = if (System.getenv("WS_ENCODER").isNullOrBlank()) System.getenv("WS_ENCODER") else "ws://encoder:8080"
    }
}