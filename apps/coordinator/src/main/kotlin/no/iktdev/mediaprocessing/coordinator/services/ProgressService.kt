package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.models.Progress
import no.iktdev.mediaprocessing.shared.common.progress.TaskProgressCache
import no.iktdev.mediaprocessing.shared.common.sse.SSEKeys
import no.iktdev.mediaprocessing.shared.common.sse.basemodel.SSEProgressUpdateEvent
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.UUID

@Component
class LocalProgressCache: TaskProgressCache() {
}

@Service
class ProgressService(
    private val cache: LocalProgressCache,
    private val sseServer: SSEServer
) {
    fun update(referenceId: UUID, taskId: UUID, progress: Progress) {
        val payload = cache.update(referenceId, taskId, progress)
        sseServer.broadcast(SSEProgressUpdateEvent(payload))
    }

}