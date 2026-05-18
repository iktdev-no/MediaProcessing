package no.iktdev.mediaprocessing.coordinator

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.CleanupPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.LanguagePreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.CoordinatorPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.MediaPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.video.VideoPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.audio.AudioPreference
import org.springframework.stereotype.Component

@Component
class Preference(
    private val coordinatorEnv: CoordinatorEnv
) {

    private val gson = Gson()
    private val lock = Any()

    // ------------------------------------------------------------
    // FULL CONFIG
    // ------------------------------------------------------------

    fun getFullConfig(): CoordinatorPreference {
        val file = coordinatorEnv.preference

        if (!file.exists()) {
            val default = defaultConfig()
            writeConfig(default)
            return default
        }

        return try {
            gson.fromJson(file.readText(), CoordinatorPreference::class.java)
                ?: defaultConfig().also { writeConfig(it) }
        } catch (e: JsonSyntaxException) {
            val fallback = defaultConfig()
            writeConfig(fallback)
            fallback
        }
    }

    fun saveFullConfig(cfg: CoordinatorPreference) {
        synchronized(lock) {
            writeConfig(cfg)
        }
    }

    // ------------------------------------------------------------
    // LANGUAGE
    // ------------------------------------------------------------

    fun getLanguagePreference(): LanguagePreference =
        getFullConfig().language

    fun saveLanguagePreference(pref: LanguagePreference) {
        synchronized(lock) {
            val cfg = getFullConfig().copy(language = pref)
            writeConfig(cfg)
        }
    }

    // ------------------------------------------------------------
    // PROCESSER
    // ------------------------------------------------------------

    fun getMediaPreference(): MediaPreference =
        getFullConfig().media

    fun saveMediaPreference(pref: MediaPreference) {
        synchronized(lock) {
            val cfg = getFullConfig().copy(media = pref)
            writeConfig(cfg)
        }
    }

    // ------------------------------------------------------------
    // Cleanup
    // ------------------------------------------------------------

    fun getCleanupPreference(): CleanupPreference =
        getFullConfig().cleanup

    fun saveCleanupPreference(pref: CleanupPreference) {
        synchronized(lock) {
            val cfg = getFullConfig().copy(cleanup = pref)
            writeConfig(cfg)
        }
    }


    // ------------------------------------------------------------
    // VIDEO
    // ------------------------------------------------------------

    fun getVideoPreference(): VideoPreference =
        getFullConfig().media.videoPreference
            ?: MediaPreference.default().videoPreference!!

    fun saveVideoPreference(pref: VideoPreference) {
        synchronized(lock) {
            val current = getFullConfig()
            val updatedProcesser = current.media.copy(videoPreference = pref)
            writeConfig(current.copy(media = updatedProcesser))
        }
    }

    // ------------------------------------------------------------
    // AUDIO
    // ------------------------------------------------------------

    fun getAudioPreference(): AudioPreference =
        getFullConfig().media.audioPreference
            ?: MediaPreference.default().audioPreference!!

    fun saveAudioPreference(pref: AudioPreference) {
        synchronized(lock) {
            val current = getFullConfig()
            val updatedProcesser = current.media.copy(audioPreference = pref)
            writeConfig(current.copy(media = updatedProcesser))
        }
    }

    // ------------------------------------------------------------
    // INTERNAL HELPERS
    // ------------------------------------------------------------

    private fun writeConfig(cfg: CoordinatorPreference) {
        val file = coordinatorEnv.preference
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(cfg))
    }

    private fun defaultConfig() = CoordinatorPreference(
        media = MediaPreference.default(),
        language = LanguagePreference.default(),
        cleanup = CleanupPreference.default()
    )
}
