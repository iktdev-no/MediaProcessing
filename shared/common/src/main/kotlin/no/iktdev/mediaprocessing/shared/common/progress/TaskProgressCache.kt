package no.iktdev.mediaprocessing.shared.common.progress

import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.serialization.ZPS.toEnvelope
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

abstract class TaskProgressCache {
    private val cache = ConcurrentHashMap<UUID, ProgressUpdate>()

    fun update(referenceId: UUID, taskId: UUID, progress: Progress): ProgressUpdate {
        val update = ProgressUpdate(referenceId.toString(), taskId.toString(), progress.toEnvelope())
        cache[taskId] = update
        return update
    }

    fun get(taskId: UUID): ProgressUpdate? = cache[taskId]

    fun getAll(): Map<UUID, ProgressUpdate> = cache.toMap()
}