package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.dto.translate.ApiResponse
import no.iktdev.mediaprocessing.coordinator.services.SequenceAggregatorService
import no.iktdev.mediaprocessing.shared.common.dto.SequenceSummary
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/sequences")
class SequenceController(
    private val aggregator: SequenceAggregatorService
) {

    @GetMapping("/active")
    fun getActive(): List<SequenceSummary> {
        return aggregator.getActiveSequences()
    }

    @GetMapping("/recent")
    fun getRecent(
        @RequestParam(defaultValue = "15") limit: Int
    ): List<SequenceSummary> {
        return aggregator.getRecentSequences(limit)
    }

    @PostMapping("/{referenceId}/continue")
    fun continueSequence(
        @PathVariable referenceId: UUID
    ): ResponseEntity<ApiResponse> {
        return try {

            val id = EventStore.createManuallyContinueEvent(referenceId)

            ResponseEntity.ok(
                ApiResponse(
                    ok = true,
                    message = "Sequence continued, event $id created!"
                )
            )

        } catch (ex: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    ApiResponse(
                        ok = false,
                        message = ex.message ?: "Unknown error"
                    )
                )
        }
    }

    @PostMapping("/{referenceId}/delete")
    fun deleteSequences(
        @PathVariable referenceId: UUID
    ): ResponseEntity<ApiResponse> {
        return try {

            val id = EventStore.deleteSequence(referenceId)

            ResponseEntity.ok(
                ApiResponse(
                    ok = true,
                    message = "Sequence deleted, Event id for deletion marking is $id"
                )
            )

        } catch (ex: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    ApiResponse(
                        ok = false,
                        message = ex.message ?: "Unknown error"
                    )
                )
        }
    }

}
