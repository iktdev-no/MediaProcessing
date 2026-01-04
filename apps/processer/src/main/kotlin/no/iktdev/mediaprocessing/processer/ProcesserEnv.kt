package no.iktdev.mediaprocessing.processer

import no.iktdev.exfl.using
import java.io.File

class ProcesserEnv {
    companion object {
        val coordinatorUrl = System.getenv("COORDINATOR_URL") ?: "http://coordinator:8080"

        val ffmpeg: String = System.getenv("SUPPORTING_EXECUTABLE_FFMPEG") ?: "ffmpeg"
        val allowOverwrite = System.getenv("ALLOW_OVERWRITE").toBoolean() ?: false

        var cachedContent: File = if (!System.getenv("DIRECTORY_CONTENT_CACHE").isNullOrBlank()) File(System.getenv("DIRECTORY_CONTENT_CACHE")) else File("/src/cache")


        val logDirectory = if (!System.getenv("LOG_DIR").isNullOrBlank()) File(System.getenv("LOG_DIR")) else
            File("data").using("logs")

        val encodeLogDirectory = logDirectory.using("encode")
        val extractLogDirectory = logDirectory.using("extract")
        val subtitleExtractLogDirectory = logDirectory.using("subtitles")

        val fullLogging = System.getenv("FullLogging").toBoolean()
    }
}