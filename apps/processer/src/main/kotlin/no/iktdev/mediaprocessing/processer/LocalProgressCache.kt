package no.iktdev.mediaprocessing.processer

import no.iktdev.eventi.models.Progress
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class LocalProgressCache {
    private val cache = ConcurrentHashMap<UUID, Progress>()

    fun update(taskId: UUID, progress: Progress) {
        cache[taskId] = progress
    }

    fun get(taskId: UUID): Progress? = cache[taskId]

    fun getAll(): Map<UUID, Progress> = cache.toMap()
}
