package no.iktdev.mediaprocessing.ui.controller.coordinator

import no.iktdev.mediaprocessing.ui.client.CoordinatorClient
import no.iktdev.mediaprocessing.ui.models.contract.preferences.CoordinatorPreference
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.LanguagePreference
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.MediaPreference
import no.iktdev.mediaprocessing.ui.models.translate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/preferences")
class PreferenceController(
    private val client: CoordinatorClient
) {

    // ------------------------------------------------------------
    // FULL CONFIG
    // ------------------------------------------------------------

    @GetMapping
    fun getFull(): Mono<CoordinatorPreference> =
        client.getFull().map { it.translate() }

    @PutMapping
    fun updateFull(@RequestBody body: CoordinatorPreference): Mono<CoordinatorPreference> {
        return client.updateFull(body.translate()).map { it.translate() }
    }


    // ------------------------------------------------------------
    // LANGUAGE
    // ------------------------------------------------------------

    @GetMapping("/language")
    fun getLanguage(): Mono<LanguagePreference> =
        client.getLanguage().map { it.translate() }

    @PutMapping("/language")
    fun updateLanguage(@RequestBody body: LanguagePreference): Mono<LanguagePreference> =
        client.updateLanguage(body.translate())
            .map { it.translate() }


    // ------------------------------------------------------------
    // PROCESSER
    // ------------------------------------------------------------

    @GetMapping("/processer")
    fun getProcesser(): Mono<MediaPreference> =
        client.getProcesser().map { it.translate() }

    @PutMapping("/processer")
    fun updateProcesser(@RequestBody body: MediaPreference): Mono<MediaPreference> =
        client.updateProcesser(body.translate()).map { it.translate() }
}