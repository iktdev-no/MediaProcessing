package no.iktdev.mediaprocessing.converter

import no.iktdev.exfl.using
import java.io.File

class ConverterEnv {
    companion object {
        val allowOverwrite = System.getenv("ALLOW_OVERWRITE").toBoolean() ?: false
        val syncDialogs = System.getenv("SYNC_DIALOGS").toBoolean()
        val outFormats: List<String> = System.getenv("OUT_FORMATS")?.split(",")?.toList() ?: emptyList()

        val logDirectory = if (!System.getenv("LOG_DIR").isNullOrBlank()) File(System.getenv("LOG_DIR")) else
            File("data").using("logs", "convert")
    }
}