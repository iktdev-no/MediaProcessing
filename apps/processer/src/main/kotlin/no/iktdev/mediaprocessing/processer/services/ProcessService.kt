package no.iktdev.mediaprocessing.processer.services

import mu.KotlinLogging
import no.iktdev.mediaprocessing.processer.config.Preference
import no.iktdev.mediaprocessing.processer.limiter.CpuLimiterFactory
import no.iktdev.mediaprocessing.processer.limiter.CpuLimiterService
import no.iktdev.mediaprocessing.shared.common.dto.processer.ProcessEntry
import no.iktdev.mediaprocessing.shared.common.dto.preference.processer.CPULimit
import no.iktdev.mediaprocessing.shared.common.dto.CpuLimitSupport
import org.springframework.stereotype.Service

@Service
class ProcessService(
    private val cpuLimiterService: CpuLimiterService = CpuLimiterFactory.create(),
    private val preference: Preference,
) {

    private val log = KotlinLogging.logger {}

    @Volatile
    private var cpuLimit: CPULimit = CPULimit.default

    private val processes = mutableListOf<ProcessEntry>()
    private val lock = Any()

    private var initialized = false

    init {
        ensureInit()
    }

    // ---------------------------
    // INIT
    // ---------------------------

    private fun ensureInit() {
        if (initialized) {
            return
        }
        initialized = true

        preference.getCpuLimit().takeIf { it.enabled }?.let {
            cpuLimit = it
        }
    }

    // ---------------------------
    // PUBLIC API
    // ---------------------------

    fun detectSupportsCpuLimits(): CpuLimitSupport =
        cpuLimiterService.detectSupportsCpuLimits()

    fun getGlobalCpuLimit(): CPULimit = cpuLimit

    fun updateCpuLimit(cpuLimit: CPULimit): Boolean {
        if (cpuLimit.enabled && cpuLimit.limit !in 1..100) {
            log.warn {
                "Attempted to set invalid CPU limit: ${cpuLimit.limit}. Must be between 1 and 100."
            }
            return false
        }

        this.cpuLimit = cpuLimit
        preference.saveCPULimit(cpuLimit)

        if (cpuLimit.enabled) {
            log.info { "CPU limit enabled at ${cpuLimit.limit}%, applying limits." }
            applyCpuLimits()
        } else {
            log.info { "CPU limit disabled, removing limits." }
            removeCpuLimits()
        }

        return true
    }

    fun addProcess(processEntry: ProcessEntry) {
        synchronized(lock) {
            log.debug { "Adding process: $processEntry to list" }
            processes.add(processEntry)
        }

        if (cpuLimit.enabled) {
            log.debug() { "Applying CPU limit of ${cpuLimit.limit}% to new process with PID ${processEntry.pid}" }
            cpuLimiterService.limitProcess(processEntry.pid, cpuLimit.limit)
        }
    }

    fun removeProcess(pid: Long) {
        synchronized(lock) {
            val entryToRemove = processes.find { it.pid == pid }
            if (entryToRemove != null) {
                log.debug() { "Removing process: $entryToRemove from list" }
                processes.remove(entryToRemove)
            }
        }

        cpuLimiterService.removeLimit(pid)
    }

    fun removeCpuLimits() {
        val snapshot = synchronized(lock) { processes.toList() }

        snapshot.forEach { proc ->
            cpuLimiterService.removeLimit(proc.pid)
        }
    }

    fun applyCpuLimits() {
        ensureInit()

        if (!cpuLimit.enabled) return

        val snapshot = synchronized(lock) { processes.toList() }

        snapshot.forEach { process ->
            log.debug("Applying CPU limit of ${cpuLimit.limit}% to process with PID ${process.pid}")
            cpuLimiterService.updateLimit(process.pid, cpuLimit.limit)
        }
    }

    fun setGlobalPinnedCores(cores: List<Int>?) =
        cpuLimiterService.setGlobalPinnedCores(cores)

    fun getGlobalPinnedCores(): List<Int>? =
        cpuLimiterService.getGlobalPinnedCores()

    fun isGlobalPinningActive(): Boolean =
        cpuLimiterService.isGlobalPinningActive()

    fun pinProcessToCores(pid: Long, cores: List<Int>) =
        cpuLimiterService.pinProcessToCores(pid, cores)

    fun getManuallyPinnedCores(pid: Long): List<Int>? =
        cpuLimiterService.getManuallyPinnedCores(pid)

    fun getAssignedCores(pid: Long): List<Int>? =
        cpuLimiterService.getAssignedCores(pid)

    fun getEffectiveCores(pid: Long): List<Int>? =
        cpuLimiterService.getEffectiveCores(pid)

    fun getPercentLimit(pid: Long): Int? =
        cpuLimiterService.getPercentLimit(pid)

    fun getProcesses(): List<ProcessEntry> =
        synchronized(lock) { processes.toList() }

}