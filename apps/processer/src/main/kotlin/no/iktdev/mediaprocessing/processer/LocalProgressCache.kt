package no.iktdev.mediaprocessing.processer

import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.serialization.ZPS.toEnvelope
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class LocalProgressCache {
    private val cache = ConcurrentHashMap<UUID, ProgressUpdate>()

    fun update(referenceId: UUID, taskId: UUID, progress: Progress): ProgressUpdate {
        val update = ProgressUpdate(referenceId.toString(), taskId.toString(), progress.toEnvelope())
        cache[taskId] = update
        return update
    }

    fun get(taskId: UUID): ProgressUpdate? = cache[taskId]

    fun getAll(): Map<UUID, ProgressUpdate> = cache.toMap()
}
