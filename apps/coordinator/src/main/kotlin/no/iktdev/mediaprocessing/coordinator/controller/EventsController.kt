package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.mediaprocessing.coordinator.services.EventService
import no.iktdev.mediaprocessing.shared.common.dto.EventQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.SequenceEvent
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/events")
class EventsController(
    private val paging: EventService
) {

    @GetMapping()
    fun getEvents(query: EventQuery): Paginated<PersistedEvent> {
        return paging.getEvents(query)
    }

    @GetMapping("/sequence/{referenceId}")
    fun getEventSequence(
        @PathVariable referenceId: UUID,
        @RequestParam(required = false) beforeEventId: UUID?,
        @RequestParam(required = false) afterEventId: UUID?,
        @RequestParam(defaultValue = "50") limit: Int
    ): List<SequenceEvent> {
        return paging.getPagedEvents(
            referenceId = referenceId,
            beforeEventId = beforeEventId,
            afterEventId = afterEventId,
            limit = limit
        )
    }

    @GetMapping("/history/{referenceId}/effective")
    fun getEffectiveHistory(
        @PathVariable referenceId: UUID,
    ): List<PersistedEvent> {
        return paging.getEffectiveHistory(referenceId)
    }
}
