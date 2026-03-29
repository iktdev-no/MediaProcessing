package no.iktdev.mediaprocessing.processer.limiter

import java.io.File

object CpuLimiterFactory {

    fun create(): CpuLimiterService {
        val os = System.getProperty("os.name").lowercase()

        val inDocker = isRunningInDocker()

        return when {
            inDocker -> LinuxCpuLimiterService() // Docker = Linux cgroups
            os.contains("linux") -> LinuxCpuLimiterService()
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
