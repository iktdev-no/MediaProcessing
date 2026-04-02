package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CpuLimitSupport

interface CpuLimiterService {
    fun limitProcess(pid: Long, percent: Int)
    fun updateLimit(pid: Long, percent: Int)
    fun removeLimit(pid: Long)

    fun detectSupportsCpuLimits(): CpuLimitSupport
}
