package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.services.EventPagingService
import no.iktdev.mediaprocessing.shared.common.dto.SequenceEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/events")
class EventsController(
    private val paging: EventPagingService
) {

    @GetMapping
    fun getEvents(
        @RequestParam referenceId: UUID,
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
