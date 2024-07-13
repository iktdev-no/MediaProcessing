package no.iktdev.mediaprocessing.converter

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.*
import no.iktdev.mediaprocessing.shared.common.persistance.ActiveMode
import no.iktdev.mediaprocessing.shared.common.persistance.RunnerManager
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.contract.data.Event
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class TaskCoordinator(): TaskCoordinatorBase() {
    private val log = KotlinLogging.logger {}
    override fun onProduceEvent(event: Event) {
        taskManager.produceEvent(event)
    }

    override fun onCoordinatorReady() {
        super.onCoordinatorReady()
        runnerManager = RunnerManager(dataSource = getEventsDatabase(), name = ConvertApplication::class.java.simpleName)
        runnerManager.assignRunner()
    }


    override val taskAvailabilityEventListener: MutableMap<TaskType, MutableList<TaskQueueListener>> = mutableMapOf(
        TaskType.Convert to mutableListOf(),
    )

    private val taskListeners: MutableSet<TaskEvents> = mutableSetOf()
    fun getTaskListeners(): List<TaskEvents> {
        return taskListeners.toList()
    }
    fun addTaskEventListener(listener: TaskEvents) {
        taskListeners.add(listener)
    }

    fun addConvertTaskListener(listener: TaskQueueListener) {
        addTaskListener(TaskType.Convert, listener)
    }

    override fun addTaskListener(type: TaskType, listener: TaskQueueListener) {
        super.addTaskListener(type, listener)
        pullForAvailableTasks()
    }


    override fun pullForAvailableTasks() {
        if (runnerManager.iAmSuperseded()) {
            // This will let the application complete but not consume new
            taskMode = ActiveMode.Passive
            return
        }
        val available = taskManager.getClaimableTasks().asClaimable()
        available.forEach { (type, list) ->
            taskAvailabilityEventListener[type]?.forEach {  listener ->
                list.foreachOrUntilClaimed {
                    listener.onTaskAvailable(it)
                }
            }
        }
    }

    override fun clearExpiredClaims() {
        val expiredClaims = taskManager.getTasksWithExpiredClaim().filter { it.task in listOf(TaskType.Convert) }
        expiredClaims.forEach {
            log.info { "Found event with expired claim: ${it.referenceId}::${it.eventId}::${it.task}" }
        }
        expiredClaims.forEach {
            val result = taskManager.deleteTaskClaim(referenceId = it.referenceId, eventId = it.eventId)
            if (result) {
                log.info { "Released claim on ${it.referenceId}::${it.eventId}::${it.task}" }
            } else {
                log.error { "Failed to release claim on ${it.referenceId}::${it.eventId}::${it.task}" }
            }
        }
    }

    interface TaskEvents {
        fun onCancelOrStopProcess(eventId: String)
    }

}
