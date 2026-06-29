package no.iktdev.mediaprocessing.coordinator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.iktdev.eventi.lifecycle.LifecycleStore
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.Result
import no.iktdev.eventi.tasks.TaskPollerImplementation
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.*

@Profile("!dev")
@Component
class TaskPollerAdministrator(
    private val taskPoller: TaskPoller,
): ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        CoroutineScope(Dispatchers.Default).launch {
            taskPoller.start()
        }
    }
}

@Profile("!notask")
@Service
class TaskPoller(
    private val reporter: TaskReporter,
    private val lifecycleStore: LifecycleStore,
) : TaskPollerImplementation(
    taskStore = TaskStore,
    lifecycleStore = lifecycleStore,
    reporterFactory = { reporter } // én reporter brukes for alle tasks
) {

}


@Component
class DefaultTaskReporter() : TaskReporter {
    override fun markClaimed(taskId: UUID, workerId: String): Result {
        return try {
            val result = TaskStore.claim(taskId, workerId)
            if (result) {
                Result.Success
            } else {
                Result.Failure("Failed to claim task $taskId for worker $workerId")
            }
        } catch (e: Exception) {
            Result.Failure("Failed to claim task $taskId for worker $workerId: ${e.message}", e ,false)
        }
    }

    override fun updateLastSeen(taskId: UUID): Result {
        return try {
            val result = TaskStore.heartbeat(taskId)
            if (result) {
                Result.Success
            } else {
                Result.Failure("Failed to update last seen for task $taskId")
            }
        } catch (e: Exception) {
            Result.Failure("Failed to update last seen for task $taskId: ${e.message}", e ,false)
        }
    }

    override fun markCompleted(taskId: UUID): Result {
        return try {
            val result = TaskStore.markConsumed(taskId, TaskStatus.Completed)
            if (result) {
                Result.Success
            } else {
                Result.Failure("Failed to mark task $taskId as completed")
            }
        } catch (e: Exception) {
            Result.Failure("Failed to mark task $taskId as completed: ${e.message}", e ,false)
        }
    }

    override fun markFailed(referenceId: UUID, taskId: UUID): Result {
        return try {
            val result = TaskStore.markConsumed(taskId, TaskStatus.Failed)
            if (result) {
                Result.Success
            } else {
                Result.Failure("Failed to mark task $taskId as failed")
            }
        } catch (e: Exception) {
            Result.Failure("Failed to mark task $taskId as failed: ${e.message}", e,false)
        }
    }

    override fun markCancelled(referenceId: UUID, taskId: UUID): Result {
        return try {
            val result = TaskStore.markConsumed(taskId, TaskStatus.Cancelled)
            if (result) {
                Result.Success
            } else {
                Result.Failure("Failed to mark task $taskId as cancelled")
            }
        } catch (e: Exception) {
            Result.Failure("Failed to mark task $taskId as cancelled: ${e.message}", e,false)
        }
    }

    override fun updateProgress(referenceId: UUID, taskId: UUID, payload: Progress): Result {
        return try {
            throw error("Updating task $taskId with payload $payload")
        } catch (e: Exception) {
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
            Result.Failure("Failed to publish event: ${e.message}", e, false)
        }
    }
}