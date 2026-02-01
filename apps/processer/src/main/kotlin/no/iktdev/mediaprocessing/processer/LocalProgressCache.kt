package no.iktdev.mediaprocessing.processer

import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class LocalProgressCache {
    private val cache = ConcurrentHashMap<UUID, FfmpegDecodedProgress>()

    fun update(taskId: UUID, progress: FfmpegDecodedProgress) {
        cache[taskId] = progress
    }

    fun get(taskId: UUID): FfmpegDecodedProgress? = cache[taskId]

    fun getAll(): Map<UUID, FfmpegDecodedProgress> = cache.toMap()
}
