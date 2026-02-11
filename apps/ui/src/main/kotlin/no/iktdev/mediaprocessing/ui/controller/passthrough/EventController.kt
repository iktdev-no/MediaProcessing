package no.iktdev.mediaprocessing.ui.controller.passthrough

import no.iktdev.mediaprocessing.shared.common.dto.EventQuery
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.DeleteResult
import no.iktdev.mediaprocessing.ui.dto.Paginated
import no.iktdev.mediaprocessing.ui.dto.UiEvent
import no.iktdev.mediaprocessing.ui.service.coordinator.CoordinatorEventService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/events")
class EventController(
    private val coordinator: CoordinatorEventService,
) {
    @GetMapping()
    fun getEvents(query: EventQuery): Mono<Paginated<UiEvent>> {
        return coordinator.getPagedEvents(query)
    }

    @GetMapping("/history/{referenceId}/effective")
    fun getEffectiveHistory(
        @PathVariable referenceId: UUID,
    ): Mono<List<UiEvent>> {
        return coordinator.getEffectiveHistory(referenceId)
    }

    @DeleteMapping("/delete/{referenceId}/{eventId}")
    fun deleteEvent(
        @PathVariable referenceId: UUID,
        @PathVariable eventId: UUID
    ): Mono<DeleteResult> {
        return coordinator.deleteEvent(referenceId, eventId)
    }
}
