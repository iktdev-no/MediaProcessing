package no.iktdev.mediaprocessing.shared.common.services

import mu.KLogger
import no.iktdev.mediaprocessing.shared.common.ClaimableTask
import no.iktdev.mediaprocessing.shared.common.TaskQueueListener
import no.iktdev.mediaprocessing.shared.common.getComputername
import no.iktdev.mediaprocessing.shared.common.task.Task
import java.io.File
import java.util.*
import javax.annotation.PostConstruct

abstract class TaskService: TaskQueueListener {
    abstract val logDir: File
    abstract val log: KLogger

    lateinit var serviceId: String

    open fun getServiceId(serviceName: String = this.javaClass.simpleName): String {
        return "${getComputername()}::${serviceName}::${UUID.randomUUID()}"
    }

    protected var assignedTask: Task? = null

    /**
     * If there is a task that the service is working on, return false
     */
    abstract fun isReadyToConsume(): Boolean
    abstract fun isTaskClaimable(task: Task): Boolean
    abstract fun onTaskAssigned(task: Task)

    abstract fun onAttachListener()

    override fun onTaskAvailable(data: ClaimableTask) {
        if (!isReadyToConsume()) {
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
        if (!isTaskClaimable(task)) {
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
        serviceId = getServiceId()
        log.info { "Starting with id: $serviceId" }
        onAttachListener()
    }

    open fun onClearTask() {
        this.assignedTask = null
    }
}