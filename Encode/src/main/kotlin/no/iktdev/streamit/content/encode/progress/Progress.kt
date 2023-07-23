package no.iktdev.streamit.content.encode.progress

data class Progress(
    val frame: Int?,
    val fps: Double?,
    val stream_0_0_q: Double?,
    val bitrate: String?,
    val total_size: Int?,
    val out_time_us: Long?,
    val out_time_ms: Long?,
    val out_time: String?,
    val dup_frames: Int?,
    val drop_frames: Int?,
    val speed: Double?,
    val progress: String?
)
