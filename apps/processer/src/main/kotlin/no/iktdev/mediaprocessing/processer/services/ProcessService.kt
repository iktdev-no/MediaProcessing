package no.iktdev.mediaprocessing.processer.services

import no.iktdev.mediaprocessing.processer.models.ProcessEntry
import org.springframework.stereotype.Service

@Service
class ProcessService {
    private val processes: MutableList<ProcessEntry> = mutableListOf()

    fun addProcess(processEntry: ProcessEntry) {
        processes.add(processEntry)
    }

    fun removeProcess(pid: Long) {
        processes.removeIf { it.pid == pid }
    }


}