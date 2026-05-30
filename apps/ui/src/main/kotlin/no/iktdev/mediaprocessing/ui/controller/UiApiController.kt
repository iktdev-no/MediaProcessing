package no.iktdev.mediaprocessing.ui.controller

import mu.KotlinLogging
import no.iktdev.mediaprocessing.ui.UiSseHub
import no.iktdev.mediaprocessing.ui.dto.requests.StartProcessRequest
import no.iktdev.mediaprocessing.ui.dto.status.SystemStatus
import no.iktdev.mediaprocessing.ui.service.CoordinatorClient
import no.iktdev.mediaprocessing.ui.service.MediaPathRewriteService
import no.iktdev.mediaprocessing.ui.service.StatusService
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api")
class UiApiController(
    private val coordinator: CoordinatorClient,
    private val statusService: StatusService,
    private val hub: UiSseHub,
    private val mediaPathRewriteService: MediaPathRewriteService,
) {
    val log = KotlinLogging.logger {}

    @PostMapping("/operations/start")
    fun startProcess(@RequestBody req: StartProcessRequest): Mono<Map<String, String>> {
        val rewritten = req.copy(fileUri = mediaPathRewriteService.rewrite(req.fileUri))
        return coordinator.startProcess(rewritten)
    }

    @GetMapping("/sse") fun events(): SseEmitter = hub.createEmitter()

    @GetMapping("/status")
    fun getStatus(): SystemStatus {
        return statusService.status
    }

    @PostMapping("/operations/cleanup/cache")
    fun cleanupCache() {
        coordinator.requestCleanupForCache()
            .doOnSuccess { log.info { "Cleanup request accepted by coordinator" } }
            .subscribe()
    }

    @PostMapping("/operations/cleanup/inbox")
    fun cleanupInbox() {
        coordinator.requestCleanupForInbox()
            .doOnSuccess { log.info { "Cleanup request accepted by coordinator" } }
            .subscribe()
    }

    @PostMapping("/operations/cleanup/cache/wipe")
    fun wipeCache() {
        coordinator.requestCacheWipe()
            .doOnSuccess { log.info { "Wipe request accepted by coordinator" } }
            .subscribe()
    }

    @PostMapping("/operations/cleanup/inbox/wipe")
    fun wipeInbox() {
        coordinator.requestInboxWipe()
            .doOnSuccess { log.info { "Wipe request accepted by coordinator" } }
            .subscribe()
    }
}
