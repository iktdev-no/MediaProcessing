package no.iktdev.streamit.content.encode

class EncodeEnv {
    companion object {
        val ffmpeg: String = System.getenv("SUPPORTING_EXECUTABLE_FFMPEG") ?: "ffmpeg"
        val allowOverwrite = System.getenv("ALLOW_OVERWRITE").toBoolean() ?: false
        val maxRunners: Int = try {System.getenv("SIMULTANEOUS_ENCODE_RUNNERS").toIntOrNull() ?: 1 } catch (e: Exception) {1}
    }
}