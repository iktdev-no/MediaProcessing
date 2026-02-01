package no.iktdev.mediaprocessing.ui.service

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.dto.*
import no.iktdev.mediaprocessing.shared.common.dto.requests.StartProcessRequest
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.ui.dto.*
import no.iktdev.mediaprocessing.ui.dto.Paginated
import no.iktdev.mediaprocessing.ui.dto.file.MediaActionType
import no.iktdev.mediaprocessing.ui.dto.health.CoordinatorHealth
import no.iktdev.mediaprocessing.ui.dto.health.DiskInfo
import no.iktdev.mediaprocessing.ui.dto.rate.EventRate
import no.iktdev.mediaprocessing.ui.dto.requests.ContinueResult
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import java.util.*

@Service
class CoordinatorClient(
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
                    items = paginated.items.map { it.toUiEvent() },
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
            .map { it.map { x -> x.toUiEvent() } }


    fun getPagedTasks(taskQuery: TaskQuery): Mono<Paginated<UiTask>> =
        coordinatorWebClient.get()
            .uri { uri ->
                uri.path("/tasks")
                    .queryParams(taskQuery.toQueryParams())
                    .build()
            }
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<Paginated<CoordinatorTaskDto>>() {})
            .map { paginatedDto ->
                Paginated(
                    items = paginatedDto.items.map { it.toUiTask() },
                    page = paginatedDto.page,
                    size = paginatedDto.size,
                    total = paginatedDto.total
                )
            }


    fun getActiveTasks(): Mono<List<UiTask>> =
        coordinatorWebClient.get()
            .uri("/tasks/active")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<CoordinatorTaskDto>>() {})
            .map { it.map { x -> x.toUiTask() } }

    fun resetTask(taskId: UUID): Mono<ResetTaskResponse> =
        coordinatorWebClient.get()
            .uri("/tasks/${taskId}/reset")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<ResetTaskResponse>() {})

    fun resetTaskForced(taskId: UUID): Mono<ResetTaskResponse> =
        coordinatorWebClient.get()
            .uri("/tasks/${taskId}/reset/force")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<ResetTaskResponse>() {})


    fun connectToSse(
        onConnected: () -> Unit,
        onDisconnected: () -> Unit,
        onReconnecting: () -> Unit
    ): Flux<ServerSentEvent<String>> {

        return coordinatorWebClient.get()
            .uri("/internal/sse")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .doOnSubscribe {
                onConnected()
            }
            .doOnError { ex ->
                onDisconnected()
                log.warn(ex) { "SSE connection lost" }
            }
            .doOnCancel {
                log.info { "SSE subscription cancelled" }
            }
            .retryWhen(
                Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                    .doBeforeRetry {
                        onReconnecting()
                        log.info { "Reconnecting to SSE..." }
                    }
            )
    }




    fun getProgress(): Mono<List<ProgressUpdate>> =
        coordinatorWebClient.get()
            .uri("/internal/progress")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<ProgressUpdate>>() {})

    fun startProcess(req: no.iktdev.mediaprocessing.ui.dto.requests.StartProcessRequest): Mono<Map<String, String>> {
        val operations: Set<OperationType> = when (req.mediaAction) {
            MediaActionType.All -> setOf(
                OperationType.Encode,
                OperationType.ExtractSubtitles,
                OperationType.ConvertSubtitles
            )
            MediaActionType.Encode -> setOf(OperationType.Encode)
            MediaActionType.ExtractSubtitles -> setOf(OperationType.ExtractSubtitles)
            MediaActionType.ConvertSubtitle -> setOf(OperationType.ConvertSubtitles)
        }


        val coordinatorRequest = StartProcessRequest(
            fileUri = req.fileUri,
            operationTypes = operations
        )
        log.info { "Starting process for fileUri=${req.fileUri} with operations=$operations" }
        return coordinatorWebClient.post()
            .uri("/operations/start")
            .bodyValue(coordinatorRequest)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<Map<String, String>>() {})
    }

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
