package no.iktdev.mediaprocessing.processer.services

import no.iktdev.mediaprocessing.processer.limiter.CpuLimiterFactory
import no.iktdev.mediaprocessing.processer.limiter.CpuLimiterService
import no.iktdev.mediaprocessing.processer.models.ProcessEntry
import org.springframework.stereotype.Service

@Service
class ProcessService(
    private val cpuLimiterService: CpuLimiterService = CpuLimiterFactory.create()
) {
    @Volatile
    private var globalCpuLimitPercent: Int = 100

    fun getGlobalCpuLimitPercent(): Int = globalCpuLimitPercent

    private val processes: MutableList<ProcessEntry> = mutableListOf()

    fun addProcess(processEntry: ProcessEntry) {
        processes.add(processEntry)
        applyCpuLimits()
    }

    fun removeProcess(pid: Long) {
        processes.removeIf { it.pid == pid }
        cpuLimiterService.removeLimit(pid)
    }

    fun setGlobalCpuLimit(percent: Int) {
        globalCpuLimitPercent = percent
        applyCpuLimits()
    }

    fun applyCpuLimits() {
        processes.forEach { process ->
            cpuLimiterService.limitProcess(process.pid, globalCpuLimitPercent)
        }
    }

}