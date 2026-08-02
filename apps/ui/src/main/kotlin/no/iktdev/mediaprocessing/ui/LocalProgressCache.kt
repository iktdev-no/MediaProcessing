package no.iktdev.mediaprocessing.ui

import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.shared.common.progress.TaskProgressCache
import no.iktdev.mediaprocessing.ui.models.contract.progress.Progress
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class LocalProgressCache {
    private val cache = ConcurrentHashMap<UUID, Progress>()

    fun get(taskId: UUID): Progress? = cache[taskId]

    fun getAll(): Map<UUID, Progress> = cache.toMap()

    fun update(progress: Progress) {
        cache[progress.taskId] = progress
    }
}
