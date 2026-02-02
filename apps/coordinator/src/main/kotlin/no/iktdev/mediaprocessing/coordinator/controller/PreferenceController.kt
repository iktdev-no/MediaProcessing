package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.PeferenceConfig
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.coordinator.ProcesserPreference
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/preference")
class PreferenceController(
    private val preference: Preference
) {

    @GetMapping
    fun getFull(): ResponseEntity<PeferenceConfig> =
        ResponseEntity.ok(preference.getFullConfig())

    @PutMapping
    fun putFull(@RequestBody body: PeferenceConfig): ResponseEntity<PeferenceConfig> {
        preference.saveFullConfig(body)
        return ResponseEntity.ok(preference.getFullConfig())
    }

    @GetMapping("/processer")
    fun getProcesser(): ResponseEntity<ProcesserPreference> =
        ResponseEntity.ok(preference.getProcesserPreference())

    @PutMapping("/processer")
    fun putProcesser(@RequestBody body: ProcesserPreference): ResponseEntity<ProcesserPreference> {
        preference.saveProcesserPreference(body)
        return ResponseEntity.ok(preference.getProcesserPreference())
    }
}
