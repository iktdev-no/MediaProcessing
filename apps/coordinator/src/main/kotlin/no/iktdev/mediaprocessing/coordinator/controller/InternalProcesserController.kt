package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.CoordinatorService
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal")
class InternalProcesserController(
    private val coordinator: CoordinatorService
) {

    @PostMapping("/progress")
    fun receiveProgress(@RequestBody update: ProgressUpdate): ResponseEntity<Void> {
        coordinator.updateProgress(update)
        return ResponseEntity.ok().build()
    }
}
