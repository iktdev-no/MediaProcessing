package no.iktdev.mediaprocessing.ui.client

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.CoordinatorPreference
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.LanguagePreference
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.MediaPreference
import no.iktdev.mediaprocessing.shared.common.dto.requests.StartProcessRequest
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.ui.models.contract.files.MediaActionType
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class CoordinatorClient(
    appsProperties: MediaProcessingAppsProperties,
    webClientFactory: WebClientFactory
) {
    val log = KotlinLogging.logger {}

    private val client = webClientFactory.create(appsProperties.coordinator.address)
    private val sseClient = webClientFactory.createSse(appsProperties.coordinator.address)

    fun streamEvents(): Flux<ServerSentEvent<String>> {
        val typeRef = object : ParameterizedTypeReference<ServerSentEvent<String>>() {}
        return sseClient.get()
            .uri("/sse")
            .retrieve()
            .bodyToFlux(typeRef)
    }

    //#region Start Process
    fun startProcess(req: no.iktdev.mediaprocessing.ui.models.contract.requests.StartProcessRequest): Mono<Map<String, String>> {
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
        return client.post()
            .uri("/operations/start")
            .bodyValue(coordinatorRequest)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<Map<String, String>>() {})
    }

    //#endregion

    //#region Preference
    fun getFull(): Mono<CoordinatorPreference> =
        client.get()
            .uri("/preference")
            .retrieve()
            .bodyToMono(CoordinatorPreference::class.java)

    fun updateFull(body: CoordinatorPreference): Mono<CoordinatorPreference> =
        client.put()
            .uri("/preference")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(CoordinatorPreference::class.java)


    // ------------------------------------------------------------
    // LANGUAGE
    // ------------------------------------------------------------

    fun getLanguage(): Mono<LanguagePreference> =
        client.get()
            .uri("/preference/language")
            .retrieve()
            .bodyToMono(LanguagePreference::class.java)

    fun updateLanguage(body: LanguagePreference): Mono<LanguagePreference> =
        client.put()
            .uri("/preference/language")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(LanguagePreference::class.java)


    // ------------------------------------------------------------
    // PROCESSER
    // ------------------------------------------------------------

    fun getProcesser(): Mono<MediaPreference> =
        client.get()
            .uri("/preference/processer")
            .retrieve()
            .bodyToMono(MediaPreference::class.java)

    fun updateProcesser(body: MediaPreference): Mono<MediaPreference> =
        client.put()
            .uri("/preference/processer")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(MediaPreference::class.java)
    //endregion

    //#region Cleanup
    fun requestCleanupForCache(): Mono<Void> {
        return client.post()
            .uri("/operations/cleanup/cache")
            .retrieve()
            .bodyToMono<Void>()
    }

    fun requestCleanupForInbox(): Mono<Void> {
        return client.post()
            .uri("/operations/cleanup/inbox")
            .retrieve()
            .bodyToMono<Void>()
    }

    fun requestCacheWipe(): Mono<Void> {
        return client.post()
            .uri("/operations/cleanup/cache/wipe")
            .retrieve()
            .bodyToMono<Void>()
    }

    fun requestInboxWipe(): Mono<Void> {
        return client.post()
            .uri("/operations/cleanup/inbox/wipe")
            .retrieve()
            .bodyToMono<Void>()
    }

    //#endregion

}
