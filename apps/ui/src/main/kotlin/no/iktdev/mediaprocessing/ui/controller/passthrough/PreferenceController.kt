package no.iktdev.mediaprocessing.ui.controller.passthrough

import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.LanguagePreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.CoordinatorPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.MediaPreference
import no.iktdev.mediaprocessing.ui.service.coordinator.MediaPreferenceService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/preferences")
class PreferenceController(
    private val coordinatorPreferences: MediaPreferenceService
) {

    // ------------------------------------------------------------
    // FULL CONFIG
    // ------------------------------------------------------------

    @GetMapping
    fun getFull(): Mono<CoordinatorPreference> =
        coordinatorPreferences.getFull()

    @PutMapping
    fun updateFull(@RequestBody body: CoordinatorPreference): Mono<CoordinatorPreference> =
        coordinatorPreferences.updateFull(body)


    // ------------------------------------------------------------
    // LANGUAGE
    // ------------------------------------------------------------

    @GetMapping("/language")
    fun getLanguage(): Mono<LanguagePreference> =
        coordinatorPreferences.getLanguage()

    @PutMapping("/language")
    fun updateLanguage(@RequestBody body: LanguagePreference): Mono<LanguagePreference> =
        coordinatorPreferences.updateLanguage(body)


    // ------------------------------------------------------------
    // PROCESSER
    // ------------------------------------------------------------

    @GetMapping("/processer")
    fun getProcesser(): Mono<MediaPreference> =
        coordinatorPreferences.getProcesser()

    @PutMapping("/processer")
    fun updateProcesser(@RequestBody body: MediaPreference): Mono<MediaPreference> =
        coordinatorPreferences.updateProcesser(body)
}
