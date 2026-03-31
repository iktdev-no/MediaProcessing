package no.iktdev.mediaprocessing.ui.service.coordinator

import mu.KotlinLogging
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.LanguagePreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.CoordinatorPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.MediaPreference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class MediaPreferenceService(
    private val coordinatorWebClient: WebClient
) {
    private val log = KotlinLogging.logger {}

    // ------------------------------------------------------------
    // FULL CONFIG
    // ------------------------------------------------------------

    fun getFull(): Mono<CoordinatorPreference> =
        coordinatorWebClient.get()
            .uri("/preference")
            .retrieve()
            .bodyToMono(CoordinatorPreference::class.java)

    fun updateFull(body: CoordinatorPreference): Mono<CoordinatorPreference> =
        coordinatorWebClient.put()
            .uri("/preference")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(CoordinatorPreference::class.java)


    // ------------------------------------------------------------
    // LANGUAGE
    // ------------------------------------------------------------

    fun getLanguage(): Mono<LanguagePreference> =
        coordinatorWebClient.get()
            .uri("/preference/language")
            .retrieve()
            .bodyToMono(LanguagePreference::class.java)

    fun updateLanguage(body: LanguagePreference): Mono<LanguagePreference> =
        coordinatorWebClient.put()
            .uri("/preference/language")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(LanguagePreference::class.java)


    // ------------------------------------------------------------
    // PROCESSER
    // ------------------------------------------------------------

    fun getProcesser(): Mono<MediaPreference> =
        coordinatorWebClient.get()
            .uri("/preference/processer")
            .retrieve()
            .bodyToMono(MediaPreference::class.java)

    fun updateProcesser(body: MediaPreference): Mono<MediaPreference> =
        coordinatorWebClient.put()
            .uri("/preference/processer")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(MediaPreference::class.java)
}
