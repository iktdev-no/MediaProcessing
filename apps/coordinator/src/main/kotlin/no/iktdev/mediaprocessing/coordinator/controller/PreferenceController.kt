package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.LanguagePreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.CoordinatorPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.MediaPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.video.VideoPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.audio.AudioPreference
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
    fun getFull(): ResponseEntity<CoordinatorPreference> =
        ResponseEntity.ok(preferenceService.getFullConfig())

    @PutMapping
    fun updateFull(@RequestBody body: CoordinatorPreference): ResponseEntity<CoordinatorPreference> {
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
    fun getProcesser(): ResponseEntity<MediaPreference> =
        ResponseEntity.ok(preferenceService.getMediaPreference())

    @PutMapping("/processer")
    fun updateProcesser(@RequestBody body: MediaPreference): ResponseEntity<MediaPreference> {
        preferenceService.saveMediaPreference(body)
        return ResponseEntity.ok(preferenceService.getMediaPreference())
    }


    // ------------------------------------------------------------
    // VIDEO
    // ------------------------------------------------------------

    @GetMapping("/video")
    fun getVideo(): ResponseEntity<VideoPreference> =
        ResponseEntity.ok(preferenceService.getMediaPreference().videoPreference)

    @PutMapping("/video")
    fun updateVideo(@RequestBody body: VideoPreference): ResponseEntity<VideoPreference> {
        preferenceService.saveVideoPreference(body)
        return ResponseEntity.ok(preferenceService.getMediaPreference().videoPreference)
    }


    // ------------------------------------------------------------
    // AUDIO
    // ------------------------------------------------------------

    @GetMapping("/audio")
    fun getAudio(): ResponseEntity<AudioPreference> =
        ResponseEntity.ok(preferenceService.getMediaPreference().audioPreference)

    @PutMapping("/audio")
    fun updateAudio(@RequestBody body: AudioPreference): ResponseEntity<AudioPreference> {
        preferenceService.saveAudioPreference(body)
        return ResponseEntity.ok(preferenceService.getMediaPreference().audioPreference)
    }
}
