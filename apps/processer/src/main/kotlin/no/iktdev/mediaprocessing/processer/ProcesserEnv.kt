package no.iktdev.mediaprocessing.processer

import no.iktdev.exfl.using
import java.io.File

class ProcesserEnv {
    companion object {
        val ffmpeg: String = System.getenv("SUPPORTING_EXECUTABLE_FFMPEG") ?: "ffmpeg"
        val allowOverwrite = System.getenv("ALLOW_OVERWRITE").toBoolean() ?: false

        val logDirectory = if (!System.getenv("LOG_DIR").isNullOrBlank()) File(System.getenv("LOG_DIR")) else
            File("data").using("logs")

        val encodeLogDirectory = logDirectory.using("encode")
        val extractLogDirectory = logDirectory.using("extract")
    }
}