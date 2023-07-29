package no.iktdev.streamit.content.encode.progress

data class Progress(
    val workId: String,
    val outFileName: String,
    val progress: Int = -1,
    val estimatedCompletion: String = "Unknown",
)