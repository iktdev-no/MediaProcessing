package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.shared.common.dto.CpuLimitSupport
import no.iktdev.mediaprocessing.shared.common.dto.WindowsCpuLimitSupport

class WindowsCpuLimiterService : CpuLimiterService {

    private fun processExists(pid: Long): Boolean =
        ProcessHandle.of(pid).map { it.isAlive }.orElse(false) ?: false

    override fun limitProcess(pid: Long, percent: Int) {
        if (percent >= 100) {
            // 100% = ingen limit → sett NORMAL og returner
            removeLimit(pid)
            return
        }

        if (!processExists(pid)) return

        val priority = when {
            percent >= 80 -> "HIGH"
            percent >= 50 -> "NORMAL"
            percent >= 20 -> "BELOW_NORMAL"
            else -> "IDLE"
        }

        ProcessBuilder(
            "powershell",
            "-Command",
            "Get-Process -Id $pid | ForEach-Object { \$_.PriorityClass = '$priority' }"
        ).start()
    }

    override fun updateLimit(pid: Long, percent: Int) {
        if (percent >= 100) {
            removeLimit(pid)
            return
        }

        if (!processExists(pid)) {
            removeLimit(pid)
            return
        }

        limitProcess(pid, percent)
    }

    override fun removeLimit(pid: Long) {
        if (!processExists(pid)) return

        ProcessBuilder(
            "powershell",
            "-Command",
            "Get-Process -Id $pid | ForEach-Object { \$_.PriorityClass = 'NORMAL' }"
        ).start()
    }

    override fun detectSupportsCpuLimits(): CpuLimitSupport {
        return WindowsCpuLimitSupport()
    }

    override fun setGlobalPinnedCores(cores: List<Int>?) {}
    override fun getGlobalPinnedCores(): List<Int>? = null
    override fun isGlobalPinningActive(): Boolean = false

    override fun pinProcessToCores(pid: Long, cores: List<Int>) {}
    override fun getManuallyPinnedCores(pid: Long): List<Int>? = null

    override fun getAssignedCores(pid: Long): List<Int>? = null
    override fun getEffectiveCores(pid: Long): List<Int>? = null
    override fun getPercentLimit(pid: Long): Int? = null

    override fun getCpuCount(): Int =
        Runtime.getRuntime().availableProcessors()
}
