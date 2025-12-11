package no.iktdev.mediaprocessing.coordinator

import java.io.File

class CoordinatorEnv {
    companion object {
        val ffprobe: String = System.getenv("SUPPORTING_EXECUTABLE_FFPROBE") ?: "ffprobe"

        val preference: File = File("/data/config/preference.json")
    }
}