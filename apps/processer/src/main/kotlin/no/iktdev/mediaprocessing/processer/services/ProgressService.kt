package no.iktdev.mediaprocessing.processer.services

import no.iktdev.eventi.models.Progress
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProgressService(
    private val cache: LocalProgressCache,
    private val sseServer: SSEServer
) {
    fun update(referenceId: UUID, taskId: UUID, progress: Progress) {
        val payload = cache.update(referenceId, taskId, progress)
        sseServer.broadcast("progress", payload)
    }

}