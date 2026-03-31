package no.iktdev.mediaprocessing.processer.services

import mu.KotlinLogging
import no.iktdev.mediaprocessing.processer.config.Preference
import no.iktdev.mediaprocessing.processer.limiter.CpuLimiterFactory
import no.iktdev.mediaprocessing.processer.limiter.CpuLimiterService
import no.iktdev.mediaprocessing.processer.models.ProcessEntry
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.processer.CPULimit
import org.springframework.stereotype.Service

@Service
class ProcessService(
    private val cpuLimiterService: CpuLimiterService = CpuLimiterFactory.create(),
    private val preference: Preference,
) {
    val log = KotlinLogging.logger {}
    @Volatile
    private var cpuLimit: CPULimit = CPULimit.default

    init {
        preference.getCpuLimit().takeIf { it.enabled }?.let {
            cpuLimit = it
        }
    }

    fun getGlobalCpuLimit(): CPULimit = cpuLimit

    fun updateCpuLimit(cpuLimit: CPULimit): Boolean {
        if (cpuLimit.enabled && (cpuLimit.limit !in 1..100)) {
            log.warn { "Attempted to set invalid CPU limit: ${cpuLimit.limit}. Must be between 1 and 100." }
            return false
        }
        this.cpuLimit = cpuLimit
        preference.saveCPULimit(cpuLimit)
        if (cpuLimit.enabled) {
            log.info { "CPU limit enabled with ${cpuLimit.limit}%, applying limits to all processes." }
            applyCpuLimits()
        } else {
            log.info { "CPU limit disabled, removing limits from all processes." }
            removeCpuLimits()
        }
        return true
    }


    private val processes: MutableList<ProcessEntry> = mutableListOf()

    fun addProcess(processEntry: ProcessEntry) {
        processes.add(processEntry)
        applyCpuLimits()
    }

    fun removeProcess(pid: Long) {
        processes.removeIf { it.pid == pid }
        cpuLimiterService.removeLimit(pid)
    }

    fun removeCpuLimits() {
        processes.forEach { proc ->
            cpuLimiterService.removeLimit(proc.pid)
        }
    }

    fun applyCpuLimits() {
        processes.forEach { process ->
            cpuLimiterService.limitProcess(process.pid, cpuLimit.limit)
        }
    }

}