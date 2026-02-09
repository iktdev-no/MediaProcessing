package no.iktdev.mediaprocessing.processer.context

import no.iktdev.mediaprocessing.ffmpeg.data.FFprobeFormat
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream

data class EncodingContext(
    val format: FFprobeFormat,
    val video: VideoStream?,
    val forceSegmented: Boolean = false,
    val forceLinear: Boolean = false,
    val segmentThresholdSeconds: Double = 300.0,   // 5 min
    val sizeThresholdBytes: Long = 500L * 1024 * 1024 // 500 MB
)
