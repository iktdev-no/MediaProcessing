package no.iktdev.streamit.content.reader.analyzer.encoding.helpers

import com.google.gson.Gson
import no.iktdev.streamit.content.reader.ReaderEnv
import org.slf4j.LoggerFactory

data class EncodingPreference(
    val video: VideoPreference,
    val audio: AudioPreference
)

data class VideoPreference(
    val codec: String = "h264",
    val pixelFormat: String = "yuv420p",
    val pixelFormatPassthrough: List<String> = listOf<String>("yuv420p", "yuv420p10le"),
    val threshold: Int = 16
)

data class AudioPreference(
    val codec: String = "aac",
    val sample_rate: Int? = null,
    val channels: Int? = null,
    val language: String = "eng", //ISO3 format
    val preserveChannels: Boolean = true,
    val defaultToEAC3OnSurroundDetected: Boolean = true,
    val forceStereo: Boolean = false
)


class PreferenceReader {
    fun getPreference(): EncodingPreference {
        val defaultPreference = EncodingPreference(video = VideoPreference(), audio = AudioPreference())
        val preferenceText = readPreference() ?: return defaultPreference
        val configured = deserialize(preferenceText)

        printConfiguration("Audio", "Codec", configured?.audio?.codec, defaultPreference.audio.codec)
        printConfiguration("Audio", "Language", configured?.audio?.language, defaultPreference.audio.language)
        printConfiguration("Audio", "Channels", configured?.audio?.channels.toString(), defaultPreference.audio.channels.toString())
        printConfiguration("Audio", "Sample rate", configured?.audio?.sample_rate.toString(), defaultPreference.audio.sample_rate.toString())
        printConfiguration("Audio", "Override to EAC3 for surround", configured?.audio?.defaultToEAC3OnSurroundDetected.toString(), defaultPreference.audio.defaultToEAC3OnSurroundDetected.toString())


        printConfiguration("Video", "Codec", configured?.video?.codec, defaultPreference.video.codec)
        printConfiguration("Video", "Pixel format", configured?.video?.pixelFormat, defaultPreference.video.pixelFormat)
        printConfiguration("Video", "Threshold", configured?.video?.threshold.toString(), defaultPreference.video.threshold.toString())


        return configured ?:  defaultPreference
    }

    fun printConfiguration(sourceType: String, key: String, value: String?, default: String?) {
        val usedValue = if (!value.isNullOrEmpty()) value else if (!default.isNullOrEmpty()) "$default (default)" else "no changes will be made"
        LoggerFactory.getLogger(javaClass.simpleName).info("$sourceType: $key => $usedValue")

    }


    fun readPreference(): String? {
        val prefFile = ReaderEnv.encodePreference
        if (!prefFile.exists()) {
            LoggerFactory.getLogger(javaClass.simpleName).info("Preference file: ${prefFile.absolutePath} does not exists...")
            LoggerFactory.getLogger(javaClass.simpleName).info("Using default configuration")
            return null
        }
        else {
            LoggerFactory.getLogger(javaClass.simpleName).info("Preference file: ${prefFile.absolutePath} found")
        }

        try {
            val instr = prefFile.inputStream()
            return instr.bufferedReader().use { it.readText() }
        }
        catch (e: Exception) {
            LoggerFactory.getLogger(javaClass.simpleName).error("Failed to read preference file: ${prefFile.absolutePath}.. Will use default configuration")
        }
        return null
    }

    fun deserialize(value: String?): EncodingPreference? {
        value ?: return null
        return Gson().fromJson(value, EncodingPreference::class.java) ?: null
    }


}