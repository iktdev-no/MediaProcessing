package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.services.ProgressManagerService
import no.iktdev.mediaprocessing.coordinator.services.SseHub
import no.iktdev.mediaprocessing.shared.common.dto.progress.Progress
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/internal")
class InternalProcesserController(
    private val progressManager: ProgressManagerService,
    private val hub: SseHub,
) {

    @PostMapping("/progress")
    fun receiveProgress(@RequestBody update: ProgressUpdate): ResponseEntity<Void> {
        val progress = progressManager.onReceivedProgressUpdate(update)
        hub.broadcast("progress", progress)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/progress")
    fun getAllProgress(): List<Progress> {
        return progressManager.getProgress()
    }

    @GetMapping("/sse")
    fun stream(): SseEmitter {
        return hub.createEmitter()
    }
}
