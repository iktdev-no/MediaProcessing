package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.ListenerOrder
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.serialization.ZDS.toTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MetadataSearchTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.requireEvent
import no.iktdev.mediaprocessing.shared.common.short
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.jetbrains.annotations.VisibleForTesting
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Component
@ListenerOrder(6)
class MediaMetadataSearchTimeOutEventListener : EventListener() {

    private val log = KotlinLogging.logger {}

    @VisibleForTesting
    internal val scheduledExpiries =
        ConcurrentHashMap<UUID, ScheduledFuture<*>>()

    private val scheduler = Executors.newScheduledThreadPool(1)

    override fun allowDerivativeOnHistoricalEvent() = true

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val taskCreated =
            history.requireEvent<MetadataSearchTaskCreatedEvent>()

        val searchResult =
            history.getInstanceOf<MetadataSearchResultEvent>()

        if (searchResult != null) {
            scheduledExpiries.remove(taskCreated.taskId)?.cancel(false)
            return null
        }

        scheduleTaskExpiry(taskCreated.taskId)

        return null
    }

    private fun scheduleTaskExpiry(taskId: UUID) {
        if (scheduledExpiries.containsKey(taskId)) {
            return
        }

        val future = scheduler.schedule({

            try {
                handleTimeout(taskId)
            } finally {
                scheduledExpiries.remove(taskId)
            }

        }, 10, TimeUnit.MINUTES)

        scheduledExpiries.putIfAbsent(taskId, future)
            ?.let {
                future.cancel(false)
            }
    }

    private fun handleTimeout(taskId: UUID) {
        val persistedTask =
            TaskStore.findByTaskId(taskId)
                ?: return

        if (persistedTask.consumed) {
            return
        }

        val task = persistedTask.toTask()

        if (task !is MetadataSearchTask) {
            return
        }

        val abandoned = TaskStore.findAbandonedTasks().any { it.taskId == taskId }

        if (!abandoned) {
            log.debug {
                "[${task.referenceId.short()}] " +
                        "Metadata search task ${task.taskId.short()} " +
                        "is still active"
            }

            return
        }

        log.warn {
            "[${task.referenceId.short()}] " +
                    "Metadata search task ${task.taskId.short()} " +
                    "has timed out and is abandoned"
        }

        val marked = TaskStore.markConsumed(
            task.taskId,
            TaskStatus.Failed
        )

        if (!marked) {
            return
        }

        val failureEvent =
            MetadataSearchResultEvent(
                status = TaskStatus.Failed
            ).apply {
                setFailed(listOf(task.taskId))
            }.producedFrom(task)

        EventStore.persist(failureEvent)
    }

}
