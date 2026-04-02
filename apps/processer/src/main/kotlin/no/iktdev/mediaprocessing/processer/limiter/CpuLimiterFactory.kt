package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.Fs
import java.io.File

object CpuLimiterFactory {

    fun create(): CpuLimiterService {
        val fs = Fs()
        val os = System.getProperty("os.name").lowercase()

        val inDocker = isRunningInDocker()

        return when {
            inDocker -> LinuxCpuLimiterService(fs) // Docker = Linux cgroups
            os.contains("linux") -> LinuxCpuLimiterService(fs)
            os.contains("windows") -> WindowsCpuLimiterService()
            else -> throw UnsupportedOperationException("Unsupported OS: $os")
        }
    }

    private fun isRunningInDocker(): Boolean {
        return File("/.dockerenv").exists() ||
               File("/proc/1/cgroup").takeIf { it.exists() }
                   ?.readText()
                   ?.contains("docker") == true
    }
}
