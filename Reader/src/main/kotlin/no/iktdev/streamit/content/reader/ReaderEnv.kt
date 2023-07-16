package no.iktdev.streamit.content.reader

import java.io.File

class ReaderEnv {
    companion object {
        val ffprobe: String = System.getenv("SUPPORTING_EXECUTABLE_FFPROBE") ?: "ffprobe"
        val encodePreference: File = File("/data/config/preference.json")
    }
}