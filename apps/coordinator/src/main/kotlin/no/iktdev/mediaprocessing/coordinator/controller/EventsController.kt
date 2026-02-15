package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.LineageNode
import no.iktdev.mediaprocessing.coordinator.services.EventService
import no.iktdev.mediaprocessing.shared.common.dto.EventQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.DeleteResult
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/events")
class EventsController(
    private val eventService: EventService
) {

    @GetMapping()
    fun getEvents(query: EventQuery): Paginated<PersistedEvent> {
        return eventService.getEvents(query)
    }

    @GetMapping("/sequence/{referenceId}")
    fun getEventSequence(
        @PathVariable referenceId: UUID,
        @RequestParam(required = false) beforeEventId: UUID?,
        @RequestParam(required = false) afterEventId: UUID?,
        @RequestParam(defaultValue = "50") limit: Int
    ): List<SequenceEvent> {
        return eventService.getPagedEvents(
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
        return eventService.getEffectiveHistory(referenceId)
    }

    @GetMapping("/{referenceId}/lineage")
    fun getLineage(
        @PathVariable referenceId: UUID,
    ): List<LineageNode> {
        return eventService.getEventsLineage(referenceId)
    }


    @DeleteMapping("/{referenceId}/{eventId}")
    fun deleteEvent(
        @PathVariable referenceId: UUID,
        @PathVariable eventId: UUID
    ): DeleteResult {
        return eventService.deleteEvent(referenceId, eventId)
    }


}
