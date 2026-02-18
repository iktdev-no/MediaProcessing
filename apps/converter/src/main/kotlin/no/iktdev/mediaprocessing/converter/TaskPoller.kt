package no.iktdev.mediaprocessing.converter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
) : TaskPollerImplementation(
    taskStore = TaskStore,
    reporterFactory = { reporter } // én reporter brukes for alle tasks
) {

}


@Component
class DefaultTaskReporter() : TaskReporter {
    override fun markClaimed(taskId: UUID, workerId: String): Result {
        return TaskStore.claim(taskId, workerId).let { result ->
            if (result) {
                Result.Success
            } else {
                Result.Failure("Failed to claim task $taskId for worker $workerId")
            }
        }
    }

    override fun updateLastSeen(taskId: UUID): Result {
        return TaskStore.heartbeat(taskId).let { result ->
            if (result) {
                Result.Success
            } else {
                Result.Failure("Failed to update heartbeat for task $taskId")
            }
        }
    }

    override fun markCompleted(taskId: UUID): Result {
        return TaskStore.markConsumed(taskId, TaskStatus.Completed).let { result ->
            if (result) {
                Result.Success
            } else {
                Result.Failure("Failed to mark task $taskId as completed")
            }
        }
    }

    override fun markFailed(referenceId: UUID, taskId: UUID): Result {
        return TaskStore.markConsumed(taskId, TaskStatus.Failed).let { result ->
            if (result) {
                Result.Success
            } else {
                Result.Failure("Failed to mark task $taskId as failed")
            }
        }
    }

    override fun markCancelled(referenceId: UUID, taskId: UUID): Result {
        return TaskStore.markConsumed(taskId, TaskStatus.Cancelled).let { result ->
            if (result) {
                Result.Success
            } else {
                Result.Failure("Failed to mark task $taskId as cancelled")
            }
        }
    }

    override fun updateProgress(
        referenceId: UUID,
        taskId: UUID,
        payload: Progress
    ): Result {
        // Not to be implemented for this application
        return Result.Failure("Progress updates not supported")
    }

    override fun log(taskId: UUID, message: String) {
        // Not to be implemented for this application
    }

    override fun publishEvent(event: Event): Result {
        return try {
            EventStore.persist(event)
            Result.Success
        } catch (e: Exception) {
            Result.Failure("Failed to publish event: ${e.message}")
        }
    }
}