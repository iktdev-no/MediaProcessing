package no.iktdev.mediaprocessing.ui.controller

import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.ui.models.contract.sequence.LifecycleNode
import no.iktdev.mediaprocessing.ui.models.contract.sequence.Sequence
import no.iktdev.mediaprocessing.ui.models.contract.sequence.SequenceSummary
import no.iktdev.mediaprocessing.ui.service.EventService
import no.iktdev.mediaprocessing.ui.service.SequenceAggregatorService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/sequences")
class SequenceController(
    private val aggregator: SequenceAggregatorService,
    private val eventService: EventService,
) {



    @GetMapping("/active")
    fun getActive(): List<Sequence> {
        return aggregator.getActiveSequences()
    }

    @GetMapping("/recent")
    fun getRecent(
        @RequestParam(defaultValue = "15") limit: Int
    ): List<Sequence> {
        return aggregator.getRecentSequences(limit)
    }

    @GetMapping("/{referenceId}")
    fun getEventSequence(
        @PathVariable referenceId: UUID,
    ): List<LifecycleNode> {
        return aggregator.generateEffectiveLifecycle(referenceId)
    }

    @GetMapping("/{referenceId}/info")
    fun getSequenceInfo(
        @PathVariable referenceId: UUID,
    ): SequenceSummary {
        return aggregator.getSequenceSummary(referenceId)
    }

    @PostMapping("/{referenceId}/continue")
    fun continueSequence(
        @PathVariable referenceId: UUID
    ): ResponseEntity<String> {
        return try {
            val id = EventStore.createManuallyContinueEvent(referenceId)
            ResponseEntity.ok("Sequence continued, event $id created!")
        } catch (ex: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.message ?: "Unknown error")
        }
    }

    @DeleteMapping("/{referenceId}")
    fun deleteSequences(
        @PathVariable referenceId: UUID
    ): ResponseEntity<String> {
        return try {
            val id = EventStore.deleteSequence(referenceId)
            ResponseEntity.ok("Sequence deleted, Event id for deletion marking is $id")
        } catch (ex: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.message ?: "Unknown error")

        }
    }

}
