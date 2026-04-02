package no.iktdev.mediaprocessing.processer.services

import no.iktdev.mediaprocessing.processer.models.SystemInfo
import no.iktdev.mediaprocessing.processer.services.fs.IFs

class SystemInfoService(
    private val fs: IFs
) {
    fun get(): SystemInfo = readSystemInfo()

    fun readSystemInfo(): SystemInfo {
        val cpuInfo = fs.readLines("/proc/cpuinfo") ?: emptyList()

        val cpuModel = cpuInfo
            .firstOrNull { it.startsWith("model name") }
            ?.substringAfter(":")
            ?.trim()

        val cpuCount = Runtime.getRuntime().availableProcessors()

        val coreCount = cpuInfo
            .firstOrNull { it.startsWith("cpu cores") }
            ?.substringAfter(":")
            ?.trim()
            ?.toIntOrNull()
            ?: cpuCount


        val loadParts = fs.readText("/proc/loadavg")
            ?.trim()
            ?.split(" ")
            ?: listOf("0", "0", "0")

        val loadAvg = Triple(
            loadParts[0].toDouble(),
            loadParts[1].toDouble(),
            loadParts[2].toDouble()
        )

        val uptimeSeconds = fs.readText("/proc/uptime")
            ?.trim()
            ?.split(" ")
            ?.getOrNull(0)
            ?.toDoubleOrNull()
            ?.toLong()
            ?: 0L

        val memInfo = fs.readLines("/proc/meminfo")
            ?.associate {
                val parts = it.split(":")
                parts[0] to parts[1].trim().split(" ")[0].toLong()
            }
            ?: emptyMap()

        val totalMem = memInfo["MemTotal"] ?: 0
        val freeMem = memInfo["MemFree"] ?: 0
        val availableMem = memInfo["MemAvailable"] ?: freeMem
        val swapTotal = memInfo["SwapTotal"] ?: 0
        val swapFree = memInfo["SwapFree"] ?: 0

        val freqs = mutableMapOf<Int, Int>()
        fs.list("/sys/devices/system/cpu")
            ?.filter { it.startsWith("cpu") && it.drop(3).toIntOrNull() != null }
            ?.forEach { cpuDir ->
                val cpu = cpuDir.drop(3).toInt()
                val freq = fs.readText("/sys/devices/system/cpu/$cpuDir/cpufreq/scaling_cur_freq")
                    ?.trim()
                    ?.toIntOrNull()
                    ?.div(1000)
                if (freq != null) freqs[cpu] = freq
            }

        val temps = mutableMapOf<String, Double>()
        fs.list("/sys/class/thermal")
            ?.filter { it.startsWith("thermal_zone") }
            ?.forEach { zone ->
                val type = fs.readText("/sys/class/thermal/$zone/type")?.trim() ?: zone
                val milli = fs.readText("/sys/class/thermal/$zone/temp")
                    ?.trim()
                    ?.toDoubleOrNull()
                if (milli != null) temps[type] = milli / 1000.0
            }

        return SystemInfo(
            cpuModel = cpuModel,
            cpuCores = coreCount,
            cpuThreads = cpuCount,
            loadAvg = loadAvg,
            uptimeSeconds = uptimeSeconds,
            totalMemKb = totalMem,
            freeMemKb = freeMem,
            availableMemKb = availableMem,
            swapTotalKb = swapTotal,
            swapFreeKb = swapFree,
            cpuFrequencies = freqs,
            temperatures = temps
        )
    }

}
