class EncodeEnv {
    companion object {
        val ffmpeg: String = System.getenv("SUPPORTING_EXECUTABLE_FFMPEG") ?: "ffmpeg"
        val allowOverwrite = System.getenv("ALLOW_OVERWRITE").toBoolean() ?: false
        val maxRunners: Int = System.getenv("SIMULTANEOUS_ENCODE_RUNNERS").toIntOrNull() ?: 1
    }
}