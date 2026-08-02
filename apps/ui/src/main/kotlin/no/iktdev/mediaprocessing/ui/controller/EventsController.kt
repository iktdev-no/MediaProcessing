package no.iktdev.mediaprocessing.ui.controller

import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.mediaprocessing.shared.common.dto.query.EventQuery
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.ui.models.contract.Paginated
import no.iktdev.mediaprocessing.ui.models.contract.Response
import no.iktdev.mediaprocessing.ui.models.contract.UiEvent
import no.iktdev.mediaprocessing.ui.models.contract.toUi
import no.iktdev.mediaprocessing.ui.models.internal.DeleteResultFailure
import no.iktdev.mediaprocessing.ui.models.internal.DeleteResultSuccess
import no.iktdev.mediaprocessing.ui.service.EventService
import no.iktdev.mediaprocessing.ui.toUiEvents
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.w3c.dom.events.UIEvent
import java.util.*

@RestController
@RequestMapping("/api/events")
class EventsController(
    private val eventService: EventService
) {

    @GetMapping("/names")
    fun getEventNames(): List<String> {
        return EventRegistry.getEvents().map { it.simpleName }
    }

    @GetMapping()
    fun getEvents(query: EventQuery): Paginated<PersistedEvent> {
        return eventService.getEvents(query).toUi()
    }

    @GetMapping("/history/{referenceId}/effective")
    fun getEffectiveHistory(
        @PathVariable referenceId: UUID,
    ): List<UiEvent> {
        return eventService.getEffectiveHistory(referenceId).toUiEvents()
    }



    @DeleteMapping("/{referenceId}/{eventId}")
    fun deleteEvent(
        @PathVariable referenceId: UUID,
        @PathVariable eventId: UUID
    ): ResponseEntity<Response> {
        return when (val success = eventService.deleteEvent(referenceId, eventId)) {
            is DeleteResultSuccess -> {
                ResponseEntity.status(HttpStatus.OK).body(Response(true))
            }
            is DeleteResultFailure -> {
                ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Response(false, success.message))
            }
        }

    }


}
