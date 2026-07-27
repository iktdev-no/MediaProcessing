package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.shared.common.dto.CpuLimitSupport

interface CpuLimiterService {
    fun limitProcess(pid: Long, percent: Int)
    fun updateLimit(pid: Long, percent: Int)
    fun removeLimit(pid: Long)

    fun detectSupportsCpuLimits(): CpuLimitSupport

    // NEW: pinning
    fun setGlobalPinnedCores(cores: List<Int>?)
    fun getGlobalPinnedCores(): List<Int>?
    fun isGlobalPinningActive(): Boolean

    fun pinProcessToCores(pid: Long, cores: List<Int>)
    fun getManuallyPinnedCores(pid: Long): List<Int>?

    fun getAssignedCores(pid: Long): List<Int>?
    fun getEffectiveCores(pid: Long): List<Int>?
    fun getPercentLimit(pid: Long): Int?

    fun getCpuCount(): Int
}
