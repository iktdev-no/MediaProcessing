package no.iktdev.streamit.content.reader

import java.io.File

class ReaderEnv {
    companion object {
        val ffprobe: String = System.getenv("SUPPORTING_EXECUTABLE_FFPROBE") ?: "ffprobe"
        val encodePreference: String? = System.getenv("ENCODE_PREFERENCE") ?: null
    }
}