package no.iktdev.mediaprocessing.shared.common

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.persistance.ActiveMode
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.contract.data.Event
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import javax.annotation.PostConstruct

@EnableScheduling
abstract class TaskCoordinatorBase() {
    private val log = KotlinLogging.logger {}
    var taskMode: ActiveMode = ActiveMode.Active
    private var ready: Boolean = false
    fun isReady() = ready

    abstract fun onProduceEvent(event: Event)


    abstract val taskAvailabilityEventListener: MutableMap<TaskType, MutableList<TaskQueueListener>>

    open fun addTaskListener(type: TaskType, listener: TaskQueueListener) {
        val listeners = taskAvailabilityEventListener[type] ?: mutableListOf<TaskQueueListener>().also { it ->
            taskAvailabilityEventListener[type] = it
        }
        listeners.add(listener)
    }


    open fun onCoordinatorReady() {
        ready = true
    }

    @PostConstruct
    fun onInitializationCompleted() {
        onCoordinatorReady()
        pullAvailable()
    }

    abstract fun pullForAvailableTasks()

    @Scheduled(fixedDelay = (5_000))
    fun pullAvailable() {
        if (taskMode != ActiveMode.Active) {
            return
        }
        pullForAvailableTasks()
    }

    abstract fun clearExpiredClaims()

    @Scheduled(fixedDelay = (300_000))
    fun resetExpiredClaims() {
        if (taskMode != ActiveMode.Active) {
            return
        }
        clearExpiredClaims()
    }
}



class ClaimableTask(private var task: Task) {
    var isConsumed: Boolean = false
        private set
    fun consume(): Task? {
        return if (!isConsumed) {
            isConsumed = true
            task
        } else null
    }
}

interface TaskQueueListener {
    fun onTaskAvailable(data: ClaimableTask)
}

fun Map<TaskType, List<Task>>.asClaimable(): Map<TaskType, List<ClaimableTask>> {
    return this.mapValues { v -> v.value.map { ClaimableTask(it) } }
}

fun List<ClaimableTask>.foreachOrUntilClaimed(block: (task: ClaimableTask) -> Unit) {
    this.forEach {
        if (!it.isConsumed) {
            block(it)
        }
    }
}
