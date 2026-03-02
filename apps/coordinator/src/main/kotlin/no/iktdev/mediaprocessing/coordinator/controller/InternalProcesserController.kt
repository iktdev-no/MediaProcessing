package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.CoordinatorService
import no.iktdev.mediaprocessing.coordinator.services.ProgressTranslatorService
import no.iktdev.mediaprocessing.coordinator.services.SseHub
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress.Progress
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/internal")
class InternalProcesserController(
    private val coordinator: CoordinatorService,
    private val hub: SseHub,
    private val progressTranslatorService: ProgressTranslatorService
) {

    @PostMapping("/progress")
    fun receiveProgress(@RequestBody update: ProgressUpdate): ResponseEntity<Void> {
        coordinator.updateProgress(update)
        val kv = progressTranslatorService.translate(update)
        hub.broadcast("progress", kv)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/progress")
    fun getAllProgress(): List<Progress> {
        return coordinator.getProgress().map { progressTranslatorService.translate(it) }
    }

    @GetMapping("/sse")
    fun stream(): SseEmitter {
        return hub.createEmitter()
    }
}
