package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.mediaprocessing.coordinator.services.EventService
import no.iktdev.mediaprocessing.shared.common.dto.query.EventQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/events")
class EventsController(
    private val eventService: EventService
) {

    @GetMapping("/names")
    fun getEventNames(): List<String> {
        return EventRegistry.getEvents().map { it.simpleName }
    }

    @GetMapping()
    fun getEvents(query: EventQuery): Paginated<PersistedEvent> {
        return eventService.getEvents(query)
    }

    @GetMapping("/history/{referenceId}/effective")
    fun getEffectiveHistory(
        @PathVariable referenceId: UUID,
    ): List<PersistedEvent> {
        return eventService.getEffectiveHistory(referenceId)
    }



    @DeleteMapping("/{referenceId}/{eventId}")
    fun deleteEvent(
        @PathVariable referenceId: UUID,
        @PathVariable eventId: UUID
    ): ResponseEntity<Boolean> {
        val success = eventService.deleteEvent(referenceId, eventId)
        val status = if (success) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(success)
    }


}
