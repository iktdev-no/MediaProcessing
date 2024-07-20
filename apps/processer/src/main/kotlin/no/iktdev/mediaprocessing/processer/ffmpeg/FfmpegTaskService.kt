package no.iktdev.mediaprocessing.processer.ffmpeg

import kotlinx.coroutines.cancel
import mu.KLogger
import no.iktdev.mediaprocessing.processer.taskManager
import no.iktdev.mediaprocessing.shared.common.ClaimableTask
import no.iktdev.mediaprocessing.shared.common.TaskQueueListener
import no.iktdev.mediaprocessing.shared.common.getComputername
import no.iktdev.mediaprocessing.shared.common.services.TaskService
import no.iktdev.mediaprocessing.shared.common.task.Task
import java.io.File
import java.util.*
import javax.annotation.PostConstruct

abstract class FfmpegTaskService: TaskService(), FfmpegListener {
    abstract override val logDir: File
    abstract override val log: KLogger

    protected var runner: FfmpegRunner? = null

    override fun onTaskAvailable(data: ClaimableTask) {
        if (runner?.isWorking() == true) {
            //log.info { "Worker is already running.., will not consume" }
            return
        }

        if (assignedTask != null) {
            log.info { "Assigned task is not unassigned.., will not consume" }
            return
        }


        val task = data.consume()
        if (task == null) {
            log.error { "Task is already consumed!" }
            return
        }
        val isAlreadyClaimed = taskManager.isTaskClaimed(referenceId = task.referenceId, eventId = task.eventId)
        if (isAlreadyClaimed) {
            log.warn { "Process is already claimed!" }
            return
        }
        task.let {
            this.assignedTask = it
            onTaskAssigned(it)
        }
    }


    @PostConstruct
    private fun onCreated() {
        log.info { "Starting with id: $serviceId" }
        onAttachListener()
    }

    fun clearWorker() {
        this.runner?.scope?.cancel()
        this.assignedTask = null
        this.runner = null
    }

    fun cancelWorkIfRunning(eventId: String) {
        if (assignedTask?.eventId == eventId) {
            runner?.cancel()
        }
    }
}