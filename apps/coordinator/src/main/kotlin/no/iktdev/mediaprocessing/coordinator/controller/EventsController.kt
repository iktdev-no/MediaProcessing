package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.services.EventPagingService
import no.iktdev.mediaprocessing.shared.common.dto.SequenceEvent
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/events")
class EventsController(
    private val paging: EventPagingService
) {


    @GetMapping("/sequence/{referenceId}")
    fun getEvents(
        @PathVariable referenceId: UUID,
        @RequestParam(required = false) beforeEventId: UUID?,
        @RequestParam(required = false) afterEventId: UUID?,
        @RequestParam(defaultValue = "50") limit: Int
    ): List<SequenceEvent> {
        return paging.getEvents(
            referenceId = referenceId,
            beforeEventId = beforeEventId,
            afterEventId = afterEventId,
            limit = limit
        )
    }
}
