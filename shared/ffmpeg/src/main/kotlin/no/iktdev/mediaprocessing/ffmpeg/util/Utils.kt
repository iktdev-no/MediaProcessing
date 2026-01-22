package no.iktdev.mediaprocessing.ffmpeg.util

import java.time.Clock
import java.time.LocalDateTime

fun UtcNow(): LocalDateTime {
    return LocalDateTime.now(Clock.systemUTC())
}