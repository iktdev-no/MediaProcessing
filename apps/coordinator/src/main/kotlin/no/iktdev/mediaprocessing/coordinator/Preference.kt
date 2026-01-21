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
        val default = ProcesserPreference.default()

        val file = coordinatorEnv.preference
        if (!file.exists()) {
            // Opprett fil med default
            file.writeText(Gson().toJson(PeferenceConfig(default)))
            return default
        }

        val text = try {
            file.readText()
        } catch (e: Exception) {
            return default
        }

        val parsed = try {
            Gson().fromJson(text, PeferenceConfig::class.java)
        } catch (e: Exception) {
            return default
        }

        // Hvis hele configen er null → default
        val cfg = parsed ?: return default

        // Hvis processer er null → default
        val p = cfg.processer ?: return default

        // Hvis underfelter er null → fyll inn default
        val safeVideo = p.videoPreference ?: default.videoPreference
        val safeAudio = p.audioPreference ?: default.audioPreference

        return ProcesserPreference(
            videoPreference = safeVideo,
            audioPreference = safeAudio
        )
    }
}


private fun File.ifExists(block: File.() -> Unit, orElse: () -> Unit = {}) {
    if (this.exists()) {
        block()
    } else {
        orElse()
    }
}
