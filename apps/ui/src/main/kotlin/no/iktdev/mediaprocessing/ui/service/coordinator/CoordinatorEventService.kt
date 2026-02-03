package no.iktdev.mediaprocessing.ui.service.coordinator

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.dto.EventQuery
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CoordinatorEventDto
import no.iktdev.mediaprocessing.ui.dto.Paginated
import no.iktdev.mediaprocessing.ui.dto.UiEvent
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.*

@Service
class CoordinatorEventService(
    private val coordinatorWebClient: WebClient,
) {
    val log = KotlinLogging.logger {}

    fun getPagedEvents(eventsQuery: EventQuery): Mono<Paginated<UiEvent>> =
        coordinatorWebClient.get()
            .uri { uri ->
                uri.path("/events")
                    .queryParams(eventsQuery.toQueryParams())
                    .build()
            }
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<Paginated<CoordinatorEventDto>>() {})
            .map { paginated ->
                Paginated(
                    items = paginated.items.map { UiEvent.from(it) },
                    page = paginated.page,
                    size = paginated.size,
                    total = paginated.total
                )
            }

    fun getEffectiveHistory(referenceId: UUID): Mono<List<UiEvent>> =
        coordinatorWebClient.get()
            .uri("/events/history/${referenceId}/effective")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<CoordinatorEventDto>>() {})
            .map { it.map { x -> UiEvent.from(x) } }


}