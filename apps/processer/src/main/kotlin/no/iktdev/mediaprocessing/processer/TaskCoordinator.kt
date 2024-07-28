package no.iktdev.mediaprocessing.processer

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.*
import no.iktdev.mediaprocessing.shared.common.database.cal.ActiveMode
import no.iktdev.mediaprocessing.shared.common.database.cal.RunnerManager
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.common.contract.data.Event
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class TaskCoordinator(): TaskCoordinatorBase() {
    private val log = KotlinLogging.logger {}
    lateinit var runnerManager: RunnerManager

    override fun onCoordinatorReady() {
        super.onCoordinatorReady()
        runnerManager = RunnerManager(dataSource = getEventsDatabase(), name = ProcesserApplication::class.java.simpleName)
        runnerManager.assignRunner()
    }


    override val taskAvailabilityEventListener: MutableMap<TaskType, MutableList<TaskQueueListener>> = mutableMapOf(
        TaskType.Encode to mutableListOf(),
        TaskType.Extract to mutableListOf()
    )

    private val taskListeners: MutableSet<TaskEvents> = mutableSetOf()
    fun getTaskListeners(): List<TaskEvents> {
        return taskListeners.toList()
    }
    fun addTaskEventListener(listener: TaskEvents) {
        taskListeners.add(listener)
    }

    fun addEncodeTaskListener(listener: TaskQueueListener) {
        addTaskListener(TaskType.Encode, listener)
    }
    fun addExtractTaskListener(listener: TaskQueueListener) {
        addTaskListener(TaskType.Extract, listener)
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

    override fun onProduceEvent(event: Event) {
        taskManager.produceEvent(event)
    }

    override fun clearExpiredClaims() {
        val expiredClaims = taskManager.getTasksWithExpiredClaim().filter { it.task in listOf(TaskType.Encode, TaskType.Extract) }
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
