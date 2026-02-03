package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.LanguagePreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.PreferenceConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.ProcesserPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.VideoPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioPreference
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/preference")
class PreferenceController(
    private val preferenceService: Preference
) {

    // ------------------------------------------------------------
    // FULL CONFIG
    // ------------------------------------------------------------

    @GetMapping
    fun getFull(): ResponseEntity<PreferenceConfig> =
        ResponseEntity.ok(preferenceService.getFullConfig())

    @PutMapping
    fun updateFull(@RequestBody body: PreferenceConfig): ResponseEntity<PreferenceConfig> {
        preferenceService.saveFullConfig(body)
        return ResponseEntity.ok(preferenceService.getFullConfig())
    }


    // ------------------------------------------------------------
    // LANGUAGE
    // ------------------------------------------------------------

    @GetMapping("/language")
    fun getLanguage(): ResponseEntity<LanguagePreference> =
        ResponseEntity.ok(preferenceService.getLanguagePreference())

    @PutMapping("/language")
    fun updateLanguage(@RequestBody body: LanguagePreference): ResponseEntity<LanguagePreference> {
        preferenceService.saveLanguagePreference(body)
        return ResponseEntity.ok(preferenceService.getLanguagePreference())
    }


    // ------------------------------------------------------------
    // PROCESSER (video + audio)
    // ------------------------------------------------------------

    @GetMapping("/processer")
    fun getProcesser(): ResponseEntity<ProcesserPreference> =
        ResponseEntity.ok(preferenceService.getProcesserPreference())

    @PutMapping("/processer")
    fun updateProcesser(@RequestBody body: ProcesserPreference): ResponseEntity<ProcesserPreference> {
        preferenceService.saveProcesserPreference(body)
        return ResponseEntity.ok(preferenceService.getProcesserPreference())
    }


    // ------------------------------------------------------------
    // VIDEO
    // ------------------------------------------------------------

    @GetMapping("/video")
    fun getVideo(): ResponseEntity<VideoPreference> =
        ResponseEntity.ok(preferenceService.getProcesserPreference().videoPreference)

    @PutMapping("/video")
    fun updateVideo(@RequestBody body: VideoPreference): ResponseEntity<VideoPreference> {
        preferenceService.saveVideoPreference(body)
        return ResponseEntity.ok(preferenceService.getProcesserPreference().videoPreference)
    }


    // ------------------------------------------------------------
    // AUDIO
    // ------------------------------------------------------------

    @GetMapping("/audio")
    fun getAudio(): ResponseEntity<AudioPreference> =
        ResponseEntity.ok(preferenceService.getProcesserPreference().audioPreference)

    @PutMapping("/audio")
    fun updateAudio(@RequestBody body: AudioPreference): ResponseEntity<AudioPreference> {
        preferenceService.saveAudioPreference(body)
        return ResponseEntity.ok(preferenceService.getProcesserPreference().audioPreference)
    }
}
