package no.iktdev.mediaprocessing.ui.service.coordinator

import mu.KotlinLogging
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CoordinatorHealth
import no.iktdev.mediaprocessing.shared.common.dto.DiskInfo
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.EventRate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class CoordinatorHealthService(
    private val coordinatorWebClient: WebClient,
) {
    val log = KotlinLogging.logger {}

    fun getHealth(): Mono<CoordinatorHealth> =
        coordinatorWebClient.get()
            .uri("/health")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<CoordinatorHealth>() {})

    fun getEventRate(): Mono<EventRate> =
        coordinatorWebClient.get()
            .uri("/health/events")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<EventRate>() {})

    fun getHealthStorage(): Mono<List<DiskInfo>> =
        coordinatorWebClient.get()
            .uri("/health/storage")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<DiskInfo>>() {})

}