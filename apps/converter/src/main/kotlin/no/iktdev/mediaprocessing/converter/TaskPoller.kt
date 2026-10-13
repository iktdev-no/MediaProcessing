package no.iktdev.mediaprocessing.converter

import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.eventi.MyTime
import no.iktdev.eventi.lifecycle.LifecycleStore
import no.iktdev.eventi.lifecycle.TaskListenerFatalError
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.Result
import no.iktdev.eventi.tasks.TaskPollerImplementation
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import no.iktdev.mediaprocessing.shared.common.coroutines.ApplicationCoroutineScope
import no.iktdev.mediaprocessing.shared.common.coroutines.runCancellablePollingLoop
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Component
class PollerAdministrator(
    private val taskPoller: TaskPoller,
    private val applicationCoroutineScope: ApplicationCoroutineScope,
): ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        applicationCoroutineScope.launch {
            taskPoller.start()
        }
    }
}


@Service
class TaskPoller(
    private val reporter: TaskReporter,
    private var lifecycleStore: LifecycleStore
) : TaskPollerImplementation(
    taskStore = TaskStore,
    lifecycleStore = lifecycleStore,
    reporterFactory = { reporter } // én reporter brukes for alle tasks
) {
    private val log = KotlinLogging.logger {}

    override suspend fun start() {
        runCancellablePollingLoop(
            pollOnce = { pollOnce() },
            backoff = { backoff },
            increaseBackoff = {
                backoff = backoff.multipliedBy(2).coerceAtMost(Duration.ofMinutes(1))
            },
            onError = { error ->
                log.error(error) { "Unhandled error in poll loop, backing off for $backoff" }
                lifecycleStore.add(
                    TaskListenerFatalError(
                        timestamp = MyTime.utcNow(),
                        taskId = "N/A",
                        listener = "TaskPoller",
                        exception = "${error::class.java.simpleName}: ${error.message}",
                        stacktrace = error.stackTraceToString()
                    )
                )
            },
            onCancellation = { cancelled ->
                log.debug { "Task poll loop cancelled: ${cancelled.message}" }
            }
        )
    }
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
