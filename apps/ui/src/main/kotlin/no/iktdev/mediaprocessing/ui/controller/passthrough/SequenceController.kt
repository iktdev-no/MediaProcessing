package no.iktdev.mediaprocessing.ui.controller.passthrough

import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceEvent
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceSummary
import no.iktdev.mediaprocessing.ui.dto.requests.ContinueResult
import no.iktdev.mediaprocessing.ui.service.coordinator.CoordinatorSequenceService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/sequences")
class SequenceController(
    private val coordinator: CoordinatorSequenceService,
) {
    @GetMapping("/active")
    fun getActive(): Mono<List<SequenceSummary>> =
        coordinator.getActiveSequences()

    @GetMapping("/recent")
    fun getRecent(@RequestParam(defaultValue = "15") limit: Int): Mono<List<SequenceSummary>> =
        coordinator.getRecentSequences(limit)

    @GetMapping()
    fun getEventSequences(
        @RequestParam referenceId: UUID,
        @RequestParam(required = false) beforeEventId: UUID?,
        @RequestParam(required = false) afterEventId: UUID?,
        @RequestParam(defaultValue = "50") limit: Int
    ): Mono<List<SequenceEvent>> =
        coordinator.getEventSequence(referenceId, beforeEventId, afterEventId, limit)

    @PostMapping("/{referenceId}/continue")
    fun continueSequence(@PathVariable referenceId: UUID): ResponseEntity<String> {
        return when (val result = coordinator.continueSequence(referenceId)) {
            is ContinueResult.Success ->
                ResponseEntity.ok("Action accepted!")

            is ContinueResult.Failure ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.message)
        }
    }

    @PostMapping("/{referenceId}/delete")
    fun deleteSequence(@PathVariable referenceId: UUID): ResponseEntity<String> {
        return when (val result = coordinator.deleteSequence(referenceId)) {
            is ContinueResult.Success ->
                ResponseEntity.ok("Action accepted!")

            is ContinueResult.Failure ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.message)
        }
    }
}