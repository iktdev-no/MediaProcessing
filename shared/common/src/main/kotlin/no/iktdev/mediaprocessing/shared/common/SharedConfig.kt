package no.iktdev.mediaprocessing.shared.common

import java.io.File

object SharedConfig {
    var incomingContent: File = if (!System.getenv("DIRECTORY_CONTENT_INCOMING").isNullOrBlank()) File(System.getenv("DIRECTORY_CONTENT_INCOMING")) else File("/src/input")
    val outgoingContent: File = if (!System.getenv("DIRECTORY_CONTENT_OUTGOING").isNullOrBlank()) File(System.getenv("DIRECTORY_CONTENT_OUTGOING")) else File("/src/output")

    val ffprobe: String = System.getenv("SUPPORTING_EXECUTABLE_FFPROBE") ?: "ffprobe"
    val ffmpeg: String = System.getenv("SUPPORTING_EXECUTABLE_FFMPEG") ?: "no/iktdev/mediaprocessing/shared/contract/ffmpeg"

    val preference: File = File("/data/config/preference.json")
}

object DatabaseConfig {
    val address: String? = System.getenv("DATABASE_ADDRESS")
    val port: String? = System.getenv("DATABASE_PORT")
    val username: String? = System.getenv("DATABASE_USERNAME")
    val password: String? = System.getenv("DATABASE_PASSWORD")
    val database: String? = System.getenv("DATABASE_NAME")
}