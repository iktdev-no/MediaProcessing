package no.iktdev.mediaprocessing.coordinator

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.shared.common.silentTry
import java.io.File

class ProcesserPreference {
    val videoPreference: VideoPreference? = null
    val audioPreference: AudioPreference? = null
}

data class VideoPreference(
    val codec: VideoCodec,
    val enforceMkv: Boolean = false
)

data class AudioPreference(
    val language: String,
    val codec: AudioCodec
)


object Preference {
    fun getProcesserPreference(): ProcesserPreference {
        var preference: ProcesserPreference = ProcesserPreference()
        CoordinatorEnv.preference.ifExists {
            val text = readText()
            try {
                val result = Gson().fromJson(text, ProcesserPreference::class.java)
                preference = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return preference
    }
}

private fun File.ifExists(block: File.() -> Unit) {
    if (this.exists()) {
        block()
    }
}
