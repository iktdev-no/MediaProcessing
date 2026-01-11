package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.CoordinatorService
import no.iktdev.mediaprocessing.coordinator.services.SseHub
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/internal")
class InternalProcesserController(
    private val coordinator: CoordinatorService,
    private val hub: SseHub
) {

    @PostMapping("/progress")
    fun receiveProgress(@RequestBody update: ProgressUpdate): ResponseEntity<Void> {
        coordinator.updateProgress(update)

        hub.broadcast("progress", update)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/progress")
    fun getAllProgress(): List<ProgressUpdate> {
        return coordinator.getProgress()
    }

    @GetMapping("/sse")
    fun stream(): SseEmitter {
        return hub.createEmitter()
    }
}
