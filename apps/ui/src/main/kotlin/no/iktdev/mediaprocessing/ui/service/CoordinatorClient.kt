package no.iktdev.mediaprocessing.ui.service

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.dto.requests.StartProcessRequest
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.ui.dto.file.MediaActionType
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Service
class CoordinatorClient(
    private val coordinatorWebClient: WebClient,
    private val sseWebClient: WebClient,
) {
    val log = KotlinLogging.logger {}


    fun connectToSse(
        onConnected: () -> Unit,
        onDisconnected: () -> Unit,
        onReconnecting: () -> Unit
    ): Flux<ServerSentEvent<Any>> =
        sseWebClient.get()
            .uri("/internal/sse")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<Any>>() {})
            .doOnSubscribe { onConnected() }
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




    fun getProgress(): Mono<List<ProgressUpdate>> =
        coordinatorWebClient.get()
            .uri("/internal/progress")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<ProgressUpdate>>() {})

    fun startProcess(req: no.iktdev.mediaprocessing.ui.dto.requests.StartProcessRequest): Mono<Map<String, String>> {
        var operationType: Set<OperationType> = emptySet()
        if (req.mediaAction.any { it == MediaActionType.All }) {
            operationType = OperationType.entries.toSet()
        } else {
            if (req.mediaAction.any { it == MediaActionType.Encode }) {
                operationType = operationType.plus(OperationType.Encode)
            }
            if (req.mediaAction.any { it == MediaActionType.ExtractSubtitles }) {
                operationType = operationType.plus(OperationType.ExtractSubtitles)
            }
            if (req.mediaAction.any { it == MediaActionType.ConvertSubtitle }) {
                operationType = operationType.plus(OperationType.ConvertSubtitles)
            }
            if (req.mediaAction.any { it == MediaActionType.MetadataSearch }) {
                operationType = operationType.plus(OperationType.MetadataSearch)
            }
        }


        val coordinatorRequest = StartProcessRequest(
            fileUri = req.fileUri,
            operationTypes = operationType
        )
        log.info { "Starting process for fileUri=${req.fileUri} with operations=$operationType" }
        return coordinatorWebClient.post()
            .uri("/operations/start")
            .bodyValue(coordinatorRequest)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<Map<String, String>>() {})
    }

    fun requestCleanupForCache(): Mono<Void> {
        return coordinatorWebClient.post()
            .uri("/operations/cleanup/cache")
            .retrieve()
            .bodyToMono<Void>()
    }

    fun requestCleanupForInbox(): Mono<Void> {
        return coordinatorWebClient.post()
            .uri("/operations/cleanup/inbox")
            .retrieve()
            .bodyToMono<Void>()
    }

    fun requestCacheWipe(): Mono<Void> {
            return coordinatorWebClient.post()
                .uri("/operations/cleanup/cache/wipe")
                .retrieve()
                .bodyToMono<Void>()
        }

    fun requestInboxWipe(): Mono<Void> {
            return coordinatorWebClient.post()
                .uri("/operations/cleanup/inbox/wipe")
                .retrieve()
                .bodyToMono<Void>()
    }


}
