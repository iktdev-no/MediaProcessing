package no.iktdev.mediaprocessing.coordinator

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException

// ------------------------------------------------------------
// MODELLER
// ------------------------------------------------------------

data class PeferenceConfig(
    val processer: ProcesserPreference,
    val language: LanguagePreference
)

data class LanguagePreference(
    val preferredAudio: List<String>,
    val preferredSubtitles: List<String>,

    val preferOriginal: Boolean = true,
    val avoidDub: Boolean = true,

    // NEW: Prioritet for hvilket subtitle-format som skal brukes som master
    val subtitleFormatPriority: List<String> = listOf("ass", "srt", "vtt", "smi"),

    // NEW: Hvordan subtitles skal velges
    val subtitleSelectionMode: SubtitleSelectionMode = SubtitleSelectionMode.DialogueOnly
) {
    companion object {
        fun default() = LanguagePreference(
            preferredAudio = listOf("eng", "nor", "jpn"),
            preferredSubtitles = listOf("eng", "nor"),
            preferOriginal = true,
            avoidDub = true,
            subtitleFormatPriority = listOf("ass", "srt", "vtt", "smi"),
            subtitleSelectionMode = SubtitleSelectionMode.DialogueOnly
        )
    }
}

enum class SubtitleSelectionMode {
    DialogueOnly,          // Kun dialog
    DialogueAndForced,     // Dialog + forced
    All                    // Alle typer
}


data class ProcesserPreference(
    val videoPreference: VideoPreference? = null,
    val audioPreference: AudioPreference? = null
) {
    companion object {
        fun default(): ProcesserPreference {
            return ProcesserPreference(
                videoPreference = VideoPreference(VideoCodec.Hevc(), false),
                audioPreference = AudioPreference(AudioCodec.Aac())
            )
        }
    }
}

data class VideoPreference(
    val codec: VideoCodec,
    val enforceMkv: Boolean = false
)

data class AudioPreference(
    val codec: AudioCodec
)

// ------------------------------------------------------------
// PREFERENCE COMPONENT
// ------------------------------------------------------------

@Component
class Preference(private val coordinatorEnv: CoordinatorEnv) {

    private val gson = Gson()
    private val lock = Any()

    /**
     * Leser hele configen, men bevarer ukjente felter.
     */
    fun getFullConfig(): PeferenceConfig {
        val file = coordinatorEnv.preference

        if (!file.exists()) {
            val default = PeferenceConfig(
                processer = ProcesserPreference.default(),
                language = LanguagePreference.default()
            )
            writeJsonObject(JsonObject().apply {
                add("processer", gson.toJsonTree(default.processer))
                add("language", gson.toJsonTree(default.language))
            }, file)
            return default
        }

        val root = try {
            JsonParser.parseString(file.readText()).asJsonObject
        } catch (e: Exception) {
            return PeferenceConfig(
                processer = ProcesserPreference.default(),
                language = LanguagePreference.default()
            )
        }

        val processer = try {
            gson.fromJson(root.get("processer"), ProcesserPreference::class.java)
                ?: ProcesserPreference.default()
        } catch (_: Exception) {
            ProcesserPreference.default()
        }

        val language = try {
            gson.fromJson(root.get("language"), LanguagePreference::class.java)
                ?: LanguagePreference.default()
        } catch (_: Exception) {
            LanguagePreference.default()
        }

        return PeferenceConfig(processer, language)
    }

    fun getProcesserPreference(): ProcesserPreference =
        getFullConfig().processer

    fun getLanguagePreference(): LanguagePreference =
        getFullConfig().language

    /**
     * Oppdaterer kun processer-delen og bevarer resten av JSON.
     */
    fun saveProcesserPreference(pref: ProcesserPreference) {
        val file = coordinatorEnv.preference
        synchronized(lock) {
            val root = readOrEmpty(file)
            root.add("processer", gson.toJsonTree(pref))
            writeJsonObject(root, file)
        }
    }

    /**
     * Oppdaterer kun language-delen og bevarer resten av JSON.
     */
    fun saveLanguagePreference(pref: LanguagePreference) {
        val file = coordinatorEnv.preference
        synchronized(lock) {
            val root = readOrEmpty(file)
            root.add("language", gson.toJsonTree(pref))
            writeJsonObject(root, file)
        }
    }

    /**
     * Overskriver hele configen (brukes hvis FE sender alt).
     */
    fun saveFullConfig(cfg: PeferenceConfig) {
        val file = coordinatorEnv.preference
        synchronized(lock) {
            val root = JsonObject()
            root.add("processer", gson.toJsonTree(cfg.processer))
            root.add("language", gson.toJsonTree(cfg.language))
            writeJsonObject(root, file)
        }
    }

    private fun readOrEmpty(file: File): JsonObject =
        if (file.exists()) {
            try {
                JsonParser.parseString(file.readText()).asJsonObject
            } catch (_: Exception) {
                JsonObject()
            }
        } else JsonObject()

    private fun writeJsonObject(obj: JsonObject, file: File) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(obj))
        } catch (e: IOException) {
            throw RuntimeException("Failed to write preference file", e)
        }
    }
}
