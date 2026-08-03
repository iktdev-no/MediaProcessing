package no.iktdev.mediaprocessing.processer.services

import mu.KotlinLogging
import no.iktdev.eventi.models.Progress
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.shared.common.sse.basemodel.SSEProgressUpdateEvent
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProgressService(
    private val cache: LocalProgressCache,
    private val sseServer: SSEServer
) {
    val log = KotlinLogging.logger {}

    fun update(referenceId: UUID, taskId: UUID, progress: Progress) {
        log.info { "Updating processor $referenceId with progress $progress for task $taskId" }
        val payload = cache.update(referenceId, taskId, progress)
        sseServer.broadcast(SSEProgressUpdateEvent(payload))
    }

}