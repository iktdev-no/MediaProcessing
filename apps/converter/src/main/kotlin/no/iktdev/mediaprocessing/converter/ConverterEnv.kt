package no.iktdev.mediaprocessing.converter

import no.iktdev.files.IFile

class ConverterEnv {
    companion object {
        val allowOverwrite = System.getenv("ALLOW_OVERWRITE").toBoolean() ?: false
        val syncDialogs = System.getenv("SYNC_DIALOGS").toBoolean()
        val outFormats: List<String> = System.getenv("OUT_FORMATS")?.split(",")?.toList() ?: emptyList()

        val logDirectory = if (!System.getenv("LOG_DIR").isNullOrBlank()) IFile(System.getenv("LOG_DIR")) else
            IFile("data").using("logs", "convert")
    }
}