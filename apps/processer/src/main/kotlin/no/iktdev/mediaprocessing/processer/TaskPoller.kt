package no.iktdev.mediaprocessing.processer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.eventi.lifecycle.LifecycleStore
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.Result
import no.iktdev.eventi.tasks.TaskPollerImplementation
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.tasks.TaskValidator
import no.iktdev.mediaprocessing.processer.services.ProgressService
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.*

@Component
class PollerAdministrator(
    private val taskPoller: TaskPoller,
): ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        CoroutineScope(Dispatchers.Default).launch {
            taskPoller.start()
        }
    }
}


@Service
class TaskPoller(
    private val reporter: TaskReporter,
    private val lifecycleStore: LifecycleStore,
    private val validator: TaskValidator
) : TaskPollerImplementation(
    taskStore = TaskStore,
    lifecycleStore = lifecycleStore,
    reporterFactory = { reporter }, // én reporter brukes for alle tasks
    validatorFactory = validator
) {

}

@Component
class TaskValidator(): TaskValidator {
    override fun isTaskValidForResult(task: Task): Boolean {
        return TaskStore.findByTaskId(task.taskId)?.consumed == false
    }
}

@Component
class DefaultTaskReporter(
    private var progressService: ProgressService,
) : TaskReporter {
    private val log = KotlinLogging.logger {}

    override fun markClaimed(taskId: UUID, workerId: String): Result {
        log.info { "$workerId claiming task $taskId" }
        return try {
            val result = TaskStore.claim(taskId, workerId)
            if (result) {
                log.info { "Successfully claimed task $taskId for worker $workerId" }
                Result.Success
            } else {
                log.warn { "Failed to claim task $taskId for worker $workerId" }
                Result.Failure("Failed to claim task $taskId for worker $workerId")
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to claim task $taskId for worker $workerId" }
            Result.Failure("Failed to claim task $taskId for worker $workerId: ${e.message}", e ,false)
        }
    }

    override fun updateLastSeen(taskId: UUID): Result {
        return try {
            val status = TaskStore.heartbeat(taskId)
            if (status) {
                log.info { "Updated heartbeat for task $taskId" }
                Result.Success
            } else {
                log.warn { "Failed to update heartbeat for task $taskId" }
                Result.Failure("Failed to update heartbeat for task $taskId")
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to update heartbeat for task $taskId" }
            Result.Failure("Failed to update heartbeat for task $taskId: ${e.message}",e ,false)
        }
    }

    override fun markCompleted(taskId: UUID): Result {
        log.info { "Marking task $taskId as completed" }
        return try {
            val result = TaskStore.markConsumed(taskId, TaskStatus.Completed)
            if (result) {
                log.info { "Successfully marked task $taskId as completed" }
                Result.Success
            } else {
                log.warn { "Failed to mark task $taskId as completed" }
                Result.Failure("Failed to mark task $taskId as completed")
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to mark task $taskId as completed" }
            Result.Failure("Failed to mark task $taskId as completed: ${e.message}", e, false)
        }
    }

    override fun markFailed(referenceId: UUID, taskId: UUID): Result {
        log.info { "Marking task $taskId as failed" }
        return try {
            val result = TaskStore.markConsumed(taskId, TaskStatus.Failed)
            if (result) {
                log.info { "Successfully marked task $taskId as failed" }
                Result.Success
            } else {
                log.warn { "Failed to mark task $taskId as failed" }
                Result.Failure("Failed to mark task $taskId as failed")
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to mark task $taskId as failed" }
            Result.Failure("Failed to mark task $taskId as failed: ${e.message}", e, false)
        }
    }

    override fun markCancelled(referenceId: UUID, taskId: UUID): Result {
        log.info { "Margin task $taskId as cancelled"}
        return try {
            val result = TaskStore.markConsumed(taskId, TaskStatus.Cancelled)
            if (result) {
                log.info { "Successfully marked task $taskId as cancelled" }
                Result.Success
            } else {
                log.warn { "Failed to mark task $taskId as cancelled" }
                Result.Failure("Failed to mark task $taskId as cancelled")
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to mark task $taskId as cancelled" }
            Result.Failure("Failed to mark task $taskId as cancelled: ${e.message}", e, false)
        }
    }

    override fun updateProgress(referenceId: UUID, taskId: UUID, payload: Progress): Result {
        return try {
            progressService.update(referenceId, taskId, progress = payload)
            Result.Success
        } catch (e: Exception) {
            log.error(e) { "Failed to update progress for task $taskId" }
            Result.Failure("Failed to update progress for task $taskId: ${e.message}", e, false)
        }
    }

    override fun log(taskId: UUID, message: String) {
        // Not to be implemented for this application
    }

    override fun publishEvent(event: Event): Result {
        return try {
            EventStore.persist(event)
            Result.Success
        } catch (e: Exception) {
            log.error(e) { "Failed to publish event ${event.eventId} for reference ${event.referenceId}" }
            Result.Failure("Failed to publish event ${event.eventId} for reference ${event.referenceId}: ${e.message}", e, false)
        }
    }
}