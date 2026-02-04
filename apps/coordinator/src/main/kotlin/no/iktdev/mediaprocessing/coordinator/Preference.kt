package no.iktdev.mediaprocessing.coordinator

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.LanguagePreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.PreferenceConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.ProcesserPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.VideoPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioPreference
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

    fun getFullConfig(): PreferenceConfig {
        val file = coordinatorEnv.preference

        if (!file.exists()) {
            val default = defaultConfig()
            writeConfig(default)
            return default
        }

        return try {
            gson.fromJson(file.readText(), PreferenceConfig::class.java)
                ?: defaultConfig().also { writeConfig(it) }
        } catch (e: JsonSyntaxException) {
            val fallback = defaultConfig()
            writeConfig(fallback)
            fallback
        }
    }

    fun saveFullConfig(cfg: PreferenceConfig) {
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

    fun getProcesserPreference(): ProcesserPreference =
        getFullConfig().processer

    fun saveProcesserPreference(pref: ProcesserPreference) {
        synchronized(lock) {
            val cfg = getFullConfig().copy(processer = pref)
            writeConfig(cfg)
        }
    }

    // ------------------------------------------------------------
    // VIDEO
    // ------------------------------------------------------------

    fun getVideoPreference(): VideoPreference =
        getFullConfig().processer.videoPreference
            ?: ProcesserPreference.default().videoPreference!!

    fun saveVideoPreference(pref: VideoPreference) {
        synchronized(lock) {
            val current = getFullConfig()
            val updatedProcesser = current.processer.copy(videoPreference = pref)
            writeConfig(current.copy(processer = updatedProcesser))
        }
    }

    // ------------------------------------------------------------
    // AUDIO
    // ------------------------------------------------------------

    fun getAudioPreference(): AudioPreference =
        getFullConfig().processer.audioPreference
            ?: ProcesserPreference.default().audioPreference!!

    fun saveAudioPreference(pref: AudioPreference) {
        synchronized(lock) {
            val current = getFullConfig()
            val updatedProcesser = current.processer.copy(audioPreference = pref)
            writeConfig(current.copy(processer = updatedProcesser))
        }
    }

    // ------------------------------------------------------------
    // INTERNAL HELPERS
    // ------------------------------------------------------------

    private fun writeConfig(cfg: PreferenceConfig) {
        val file = coordinatorEnv.preference
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(cfg))
    }

    private fun defaultConfig() = PreferenceConfig(
        processer = ProcesserPreference.default(),
        language = LanguagePreference.default()
    )
}
