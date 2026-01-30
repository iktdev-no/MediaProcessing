package no.iktdev.mediaprocessing.processer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
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
    private val log = KotlinLogging.logger {}

    override fun markClaimed(taskId: UUID, workerId: String) {
        log.info { "$workerId claiming task $taskId" }
        TaskStore.claim(taskId, workerId)
    }

    override fun updateLastSeen(taskId: UUID) {
        TaskStore.heartbeat(taskId)
    }

    override fun markCompleted(taskId: UUID) {
        log.info { "Marking task $taskId as completed" }
        TaskStore.markConsumed(taskId, TaskStatus.Completed)
    }

    override fun markFailed(taskId: UUID) {
        log.info { "Marking task $taskId as failed" }
        TaskStore.markConsumed(taskId, TaskStatus.Failed)
    }

    override fun markCancelled(taskId: UUID) {
        log.info { "Margin task $taskId as cancelled"}
        TaskStore.markConsumed(taskId, TaskStatus.Cancelled)
    }

    override fun updateProgress(taskId: UUID, progress: Int) {
        // Not to be implemented for this application
    }

    override fun log(taskId: UUID, message: String) {
        // Not to be implemented for this application
    }

    override fun publishEvent(event: Event) {
        EventStore.persist(event)
    }
}