package no.iktdev.streamit.content.encode

import no.iktdev.exfl.using
import java.io.File

class ProcesserEnv {
    companion object {
        val ffmpeg: String = System.getenv("SUPPORTING_EXECUTABLE_FFMPEG") ?: "ffmpeg"
        val allowOverwrite = System.getenv("ALLOW_OVERWRITE").toBoolean() ?: false
        val maxEncodeRunners: Int = try {System.getenv("SIMULTANEOUS_ENCODE_RUNNERS").toIntOrNull() ?: 1 } catch (e: Exception) {1}
        val maxExtractRunners: Int = try {System.getenv("SIMULTANEOUS_EXTRACT_RUNNERS").toIntOrNull() ?: 1 } catch (e: Exception) {1}

        val logDirectory = if (!System.getenv("LOG_DIR").isNullOrBlank()) File(System.getenv("LOG_DIR")) else
            File("data").using("logs")
    }
}