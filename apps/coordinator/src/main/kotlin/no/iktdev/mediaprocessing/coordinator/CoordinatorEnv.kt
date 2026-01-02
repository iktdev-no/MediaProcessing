package no.iktdev.mediaprocessing.coordinator

import java.io.File

class CoordinatorEnv {
    companion object {
        val streamitAddress = System.getenv("STREAMIT_ADDRESS") ?: "http://streamit.service"

        val ffprobe: String = System.getenv("SUPPORTING_EXECUTABLE_FFPROBE") ?: "ffprobe"

        val preference: File = File("/data/config/preference.json")

        var cachedContent: File = if (!System.getenv("DIRECTORY_CONTENT_CACHE").isNullOrBlank()) File(System.getenv("DIRECTORY_CONTENT_CACHE")) else File("/src/cache")
        val outgoingContent: File = if (!System.getenv("DIRECTORY_CONTENT_OUTGOING").isNullOrBlank()) File(System.getenv("DIRECTORY_CONTENT_OUTGOING")) else File("/src/output")

    }
}