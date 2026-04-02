package no.iktdev.mediaprocessing.processer.services

import no.iktdev.mediaprocessing.processer.models.SystemInfo
import no.iktdev.mediaprocessing.processer.services.fs.FakeFs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SystemInfoServiceTest {

    @Test
    fun `system info parses cpu, memory, load and uptime correctly`() {
        val fs = FakeFs()

        // --- CPU INFO ---
        fs.mkdirs("/proc")
        fs.writeText(
            "/proc/cpuinfo",
            """
            processor   : 0
            model name  : Fake CPU 9000
            cpu cores   : 4
            """.trimIndent()
        )

        // --- LOAD AVG ---
        fs.writeText("/proc/loadavg", "0.10 0.20 0.30")

        // --- UPTIME ---
        fs.writeText("/proc/uptime", "12345.67 0.00")

        // --- MEMORY ---
        fs.writeText(
            "/proc/meminfo",
            """
            MemTotal:       8000000 kB
            MemFree:        2000000 kB
            MemAvailable:   5000000 kB
            SwapTotal:      2000000 kB
            SwapFree:       1500000 kB
            """.trimIndent()
        )

        // --- CPU FREQUENCIES ---
        fs.mkdirs("/sys/devices/system/cpu/cpu0/cpufreq")
        fs.mkdirs("/sys/devices/system/cpu/cpu1/cpufreq")

        fs.writeText("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "4200000")
        fs.writeText("/sys/devices/system/cpu/cpu1/cpufreq/scaling_cur_freq", "4100000")

        // --- TEMPERATURES ---
        fs.mkdirs("/sys/class/thermal/thermal_zone0")

        fs.writeText("/sys/class/thermal/thermal_zone0/type", "CPU")
        fs.writeText("/sys/class/thermal/thermal_zone0/temp", "55000")

        val service = SystemInfoService(fs)
        val info = service.get()

        // --- ASSERT CPU ---
        assertEquals("Fake CPU 9000", info.cpuModel)
        assertEquals(4, info.cpuCores)
        assertEquals(Runtime.getRuntime().availableProcessors(), info.cpuThreads)

        // --- ASSERT LOAD ---
        assertEquals(0.10, info.loadAvg.first)
        assertEquals(0.20, info.loadAvg.second)
        assertEquals(0.30, info.loadAvg.third)

        // --- ASSERT UPTIME ---
        assertEquals(12345L, info.uptimeSeconds)

        // --- ASSERT MEMORY ---
        assertEquals(8000000, info.totalMemKb)
        assertEquals(2000000, info.freeMemKb)
        assertEquals(5000000, info.availableMemKb)
        assertEquals(2000000, info.swapTotalKb)
        assertEquals(1500000, info.swapFreeKb)

        // --- ASSERT CPU FREQUENCIES ---
        assertEquals(4200, info.cpuFrequencies[0])
        assertEquals(4100, info.cpuFrequencies[1])

        // --- ASSERT TEMPERATURES ---
        assertEquals(55.0, info.temperatures["CPU"])
    }
}
