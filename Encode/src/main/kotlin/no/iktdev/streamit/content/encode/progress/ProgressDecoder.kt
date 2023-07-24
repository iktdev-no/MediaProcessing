package no.iktdev.streamit.content.encode.progress

class ProgressDecoder {
    fun parseVideoProgress(lines: List<String>): Progress? {
        var frame: Int? = null
        var progress: String? = null
        val metadataMap = mutableMapOf<String, String>()

        for (line in lines) {
            val keyValuePairs = Regex("=\\s*").replace(line, "=").split(" ").filter { it.isNotBlank() }
            for (keyValuePair in keyValuePairs) {
                val (key, value) = keyValuePair.split("=")
                metadataMap[key] = value
            }

            if (frame == null) {
                frame = metadataMap["frame"]?.toIntOrNull()
            }

            progress = metadataMap["progress"]
        }

        return if (progress != null) {
            // When "progress" is found, build and return the VideoMetadata object
            Progress(
                frame, metadataMap["fps"]?.toDoubleOrNull(), metadataMap["stream_0_0_q"]?.toDoubleOrNull(),
                metadataMap["bitrate"], metadataMap["total_size"]?.toIntOrNull(), metadataMap["out_time_us"]?.toLongOrNull(),
                metadataMap["out_time_ms"]?.toLongOrNull(), metadataMap["out_time"], metadataMap["dup_frames"]?.toIntOrNull(),
                metadataMap["drop_frames"]?.toIntOrNull(), metadataMap["speed"]?.toDoubleOrNull(), progress
            )
        } else {
            null // If "progress" is not found, return null
        }
    }
}