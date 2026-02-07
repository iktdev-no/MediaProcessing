package no.iktdev.mediaprocessing.ffmpeg.data

data class FFprobeFormat(
    val filename: String,
    val nb_streams: Int,
    val nb_programs: Int,
    val format_name: String,
    val format_long_name: String,
    val start_time: String,
    val duration: String,
    val size: String,
    val bit_rate: String,
    val probe_score: Int,
    val tags: Map<String, String>? = null
) {
}
