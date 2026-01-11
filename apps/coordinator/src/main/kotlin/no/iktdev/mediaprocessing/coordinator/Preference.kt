package no.iktdev.mediaprocessing.coordinator

import com.google.gson.Gson
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import org.springframework.stereotype.Component
import java.io.File


data class PeferenceConfig(
    val processer: ProcesserPreference
)

data class ProcesserPreference(
    val videoPreference: VideoPreference? = null,
    val audioPreference: AudioPreference? = null
) {
    companion object {
        fun default(): ProcesserPreference {
            return ProcesserPreference(
                videoPreference = VideoPreference(VideoCodec.Hevc(), false),
                audioPreference = AudioPreference("jpn", AudioCodec.Aac())
            )
        }
    }
}

data class VideoPreference(
    val codec: VideoCodec,
    val enforceMkv: Boolean = false
)

data class AudioPreference(
    val language: String? = null,
    val codec: AudioCodec
)

@Component
class Preference(private val coordinatorEnv: CoordinatorEnv) {
    fun getProcesserPreference(): ProcesserPreference {
        var preference: ProcesserPreference = ProcesserPreference.default()
        coordinatorEnv.preference.ifExists({
            val text = readText()
            try {
                val result = Gson().fromJson(text, PeferenceConfig::class.java)
                preference = result.processer
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, orElse = {
            coordinatorEnv.preference.writeText(Gson().toJson(PeferenceConfig(preference)))
        })
        return preference
    }
}

private fun File.ifExists(block: File.() -> Unit, orElse: () -> Unit = {}) {
    if (this.exists()) {
        block()
    } else {
        orElse()
    }
}
