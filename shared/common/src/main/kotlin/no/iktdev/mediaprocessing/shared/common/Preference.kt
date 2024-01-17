package no.iktdev.mediaprocessing.shared.common

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.PreferenceDto

private val log = KotlinLogging.logger {}
class Preference {

    companion object {
        private var prevPreference: PreferenceDto? = null
        fun getPreference(): PreferenceDto {
            val preference = readOrDefaultPreference()
            if (preference != prevPreference) {
                log.info { "[Audio]: Codec = " + preference.encodePreference.audio.codec }
                log.info { "[Audio]: Language = " + preference.encodePreference.audio.language }
                log.info { "[Audio]: Channels = " + preference.encodePreference.audio.channels }
                log.info { "[Audio]: Sample rate = " + preference.encodePreference.audio.sample_rate }
                log.info { "[Audio]: Use EAC3 for surround = " + preference.encodePreference.audio.defaultToEAC3OnSurroundDetected }

                log.info { "[Video]: Codec = " + preference.encodePreference.video.codec }
                log.info { "[Video]: Pixel format = " + preference.encodePreference.video.pixelFormat }
                log.info { "[Video]: Pixel format pass-through = " + preference.encodePreference.video.pixelFormatPassthrough.joinToString(", ")  }
                log.info { "[Video]: Threshold = " + preference.encodePreference.video.threshold }
            }
            return preference.also { prevPreference = it }
        }

        private fun readOrDefaultPreference(): PreferenceDto {
            val preference = readPreferenceFromFile() ?: PreferenceDto()
            return preference
        }


        private fun readPreferenceFromFile(): PreferenceDto? {
            val prefFile = SharedConfig.preference
            if (!prefFile.exists()) {
                log.info("Preference file: ${prefFile.absolutePath} does not exists...")
                log.info("Using default configuration")
                val gson = GsonBuilder().setPrettyPrinting().create()
                SharedConfig.preference.printWriter().use { out ->
                    out.print(gson.toJson(PreferenceDto()))
                }
                return null
            }
            else {
                log.info("Preference file: ${prefFile.absolutePath} found")
            }

            return try {
                val instr = prefFile.inputStream()
                val text = instr.bufferedReader().use { it.readText() }
                Gson().fromJson(text, PreferenceDto::class.java)
            }
            catch (e: Exception) {
                log.error("Failed to read preference file: ${prefFile.absolutePath}.. Will use default configuration")
                null
            }
        }
    }





}