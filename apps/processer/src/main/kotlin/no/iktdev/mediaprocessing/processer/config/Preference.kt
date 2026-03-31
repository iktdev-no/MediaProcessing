package no.iktdev.mediaprocessing.processer.config

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.processer.CPULimit
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.processer.ProcessorPreference
import org.springframework.stereotype.Component

@Component
class Preference(
    val props: ProcesserProperties
) {
    private val gson = Gson()
    private val lock = Any()

    fun getConfig(): ProcessorPreference {
        val file = props.preference

        if (!file.exists()) {
            val default = defaultConfig()
            writeConfig(default)
            return default
        }

        return try {
            gson.fromJson(file.readText(), ProcessorPreference::class.java)
                ?: defaultConfig().also { writeConfig(it) }
        } catch (e: JsonSyntaxException) {
            val fallback = defaultConfig()
            writeConfig(fallback)
            fallback
        }
    }


    fun getCpuLimit(): CPULimit {
        return getConfig().cpuLimit
    }

    fun saveCPULimit(pref: CPULimit) {
        synchronized(lock) {
            val current = getConfig()
            val updatedCpuLimit = current.copy(cpuLimit = pref)
            writeConfig(updatedCpuLimit)
        }
    }

    fun saveFullConfig(cfg: ProcessorPreference) {
        synchronized(lock) {
            writeConfig(cfg)
        }
    }

    private fun writeConfig(cfg: ProcessorPreference) {
        val file = props.preference
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(cfg))
    }

    private fun defaultConfig() = ProcessorPreference(
        CPULimit.default
    )
}