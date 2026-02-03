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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Service
class CoordinatorClient(
    private val coordinatorWebClient: WebClient,
) {
    val log = KotlinLogging.logger {}



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
                OperationType.ConvertSubtitles,
                OperationType.MetadataSearch
            )
            MediaActionType.Encode -> setOf(OperationType.Encode)
            MediaActionType.ExtractSubtitles -> setOf(OperationType.ExtractSubtitles)
            MediaActionType.ConvertSubtitle -> setOf(OperationType.ConvertSubtitles)
            MediaActionType.MetadataSearch -> setOf(OperationType.MetadataSearch)
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






}
