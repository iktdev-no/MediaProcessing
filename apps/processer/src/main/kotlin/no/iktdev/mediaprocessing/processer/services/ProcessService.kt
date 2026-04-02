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
        if (initialized) return
        initialized = true

        preference.getCpuLimit().takeIf { it.enabled }?.let {
            cpuLimit = it
        }
    }

    // ---------------------------
    // PUBLIC API
    // ---------------------------

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
            processes.add(processEntry)
        }

        if (cpuLimit.enabled) {
            cpuLimiterService.limitProcess(processEntry.pid, cpuLimit.limit)
        }
    }

    fun removeProcess(pid: Long) {
        synchronized(lock) {
            processes.removeIf { it.pid == pid }
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
            cpuLimiterService.updateLimit(process.pid, cpuLimit.limit)
        }
    }
}