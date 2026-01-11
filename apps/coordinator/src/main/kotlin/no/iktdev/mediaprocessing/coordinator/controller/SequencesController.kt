package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.services.SequenceAggregatorService
import no.iktdev.mediaprocessing.shared.common.dto.SequenceSummary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
}
