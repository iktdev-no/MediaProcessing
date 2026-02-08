package no.iktdev.mediaprocessing.ui.service.coordinator

import mu.KotlinLogging
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.LanguagePreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.PreferenceConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.ProcesserPreference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class CoordinatorPreferenceService(
    private val coordinatorWebClient: WebClient
) {
    private val log = KotlinLogging.logger {}

    // ------------------------------------------------------------
    // FULL CONFIG
    // ------------------------------------------------------------

    fun getFull(): Mono<PreferenceConfig> =
        coordinatorWebClient.get()
            .uri("/preference")
            .retrieve()
            .bodyToMono(PreferenceConfig::class.java)

    fun updateFull(body: PreferenceConfig): Mono<PreferenceConfig> =
        coordinatorWebClient.put()
            .uri("/preference")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(PreferenceConfig::class.java)


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

    fun getProcesser(): Mono<ProcesserPreference> =
        coordinatorWebClient.get()
            .uri("/preference/processer")
            .retrieve()
            .bodyToMono(ProcesserPreference::class.java)

    fun updateProcesser(body: ProcesserPreference): Mono<ProcesserPreference> =
        coordinatorWebClient.put()
            .uri("/preference/processer")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(ProcesserPreference::class.java)
}
