package no.iktdev.mediaprocessing.ui.controller.passthrough

import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.LanguagePreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.PreferenceConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.ProcesserPreference
import no.iktdev.mediaprocessing.ui.service.coordinator.CoordinatorPreferenceService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/preferences")
class PreferenceController(
    private val coordinatorPreferences: CoordinatorPreferenceService
) {

    // ------------------------------------------------------------
    // FULL CONFIG
    // ------------------------------------------------------------

    @GetMapping
    fun getFull(): Mono<PreferenceConfig> =
        coordinatorPreferences.getFull()

    @PutMapping
    fun updateFull(@RequestBody body: PreferenceConfig): Mono<PreferenceConfig> =
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
    fun getProcesser(): Mono<ProcesserPreference> =
        coordinatorPreferences.getProcesser()

    @PutMapping("/processer")
    fun updateProcesser(@RequestBody body: ProcesserPreference): Mono<ProcesserPreference> =
        coordinatorPreferences.updateProcesser(body)
}
