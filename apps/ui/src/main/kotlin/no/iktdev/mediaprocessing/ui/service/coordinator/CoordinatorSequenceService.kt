package no.iktdev.mediaprocessing.ui.service.coordinator

import mu.KotlinLogging
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceEvent
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceSummary
import no.iktdev.mediaprocessing.ui.dto.requests.ContinueResult
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.*

@Service
class CoordinatorSequenceService(
    private val coordinatorWebClient: WebClient,
) {
    val log = KotlinLogging.logger {}

    fun getEventSequence(
        referenceId: UUID,
        beforeEventId: UUID? = null,
        afterEventId: UUID? = null,
        limit: Int = 50
    ): Mono<List<SequenceEvent>> =
        coordinatorWebClient.get()
            .uri { uri ->
                uri.path("/events")
                    .queryParam("referenceId", referenceId)
                    .apply {
                        beforeEventId?.let { queryParam("beforeEventId", it) }
                        afterEventId?.let { queryParam("afterEventId", it) }
                    }
                    .queryParam("limit", limit)
                    .build()
            }
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<SequenceEvent>>() {})



    fun getActiveSequences(): Mono<List<SequenceSummary>> =
        coordinatorWebClient.get()
            .uri("/sequences/active")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<SequenceSummary>>() {})

    fun getRecentSequences(limit: Int = 15): Mono<List<SequenceSummary>> =
        coordinatorWebClient.get()
            .uri("/sequences/recent?limit=$limit")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<SequenceSummary>>() {})

    fun continueSequence(referenceId: UUID): ContinueResult {
        return try {
            coordinatorWebClient.post()
                .uri { it.path("/sequences/{id}/continue").build(referenceId.toString()) }
                .retrieve()
                .onStatus({ it.is4xxClientError }) { resp ->
                    resp.bodyToMono(String::class.java)
                        .flatMap { msg -> Mono.error(RuntimeException("Client error: $msg")) }
                }
                .onStatus({ it.is5xxServerError }) { resp ->
                    resp.bodyToMono(String::class.java)
                        .flatMap { msg -> Mono.error(RuntimeException("Server error: $msg")) }
                }
                .bodyToMono(Void::class.java)
                .block()

            ContinueResult.Success

        } catch (ex: Exception) {
            ContinueResult.Failure(ex.message ?: "Unknown error")
        }
    }
    fun deleteSequence(referenceId: UUID): ContinueResult {
        return try {
            coordinatorWebClient.post()
                .uri { it.path("/sequences/{id}/delete").build(referenceId.toString()) }
                .retrieve()
                .onStatus({ it.is4xxClientError }) { resp ->
                    resp.bodyToMono(String::class.java)
                        .flatMap { msg -> Mono.error(RuntimeException("Client error: $msg")) }
                }
                .onStatus({ it.is5xxServerError }) { resp ->
                    resp.bodyToMono(String::class.java)
                        .flatMap { msg -> Mono.error(RuntimeException("Server error: $msg")) }
                }
                .bodyToMono(Void::class.java)
                .block()

            ContinueResult.Success

        } catch (ex: Exception) {
            ContinueResult.Failure(ex.message ?: "Unknown error")
        }
    }
}