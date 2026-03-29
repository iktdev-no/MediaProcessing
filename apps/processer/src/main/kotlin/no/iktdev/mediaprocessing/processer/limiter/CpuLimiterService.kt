package no.iktdev.mediaprocessing.processer.limiter

interface CpuLimiterService {
    fun limitProcess(pid: Long, percent: Int)
    fun updateLimit(pid: Long, percent: Int)
    fun removeLimit(pid: Long)
}
