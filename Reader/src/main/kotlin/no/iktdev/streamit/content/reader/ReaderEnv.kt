package no.iktdev.streamit.content.reader

import java.io.File

class ReaderEnv {
    companion object {
        val metadataTimeOut: Long = System.getenv("TIMEOUT_READER_WAIT_FOR_METADATA").toLongOrNull() ?: 300000
        val ffprobe: String = System.getenv("SUPPORTING_EXECUTABLE_FFPROBE") ?: "ffprobe"
        val encodePreference: File = File("/data/config/preference.json")
    }
}