package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.ListenerOrder
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MetadataSearchTask
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.jetbrains.annotations.VisibleForTesting
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Component
@ListenerOrder(5)
class MediaCreateMetadataSearchTaskListener: EventListener() {

    @VisibleForTesting
    internal val scheduledExpiries = ConcurrentHashMap<UUID, ScheduledFuture<*>>()
    private val scheduler = Executors.newScheduledThreadPool(1)

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        // For replay
        if (event is MetadataSearchTaskCreatedEvent) {
            val hasResult = history.filter { it is MetadataSearchResultEvent }
                .any { it.metadata.derivedFromId?.contains(event.taskId) == true }

            if (!hasResult) {
                scheduleTaskExpiry(event.taskId, event.eventId, event.referenceId)
            }
        } else if (event is MetadataSearchResultEvent) {
            val cancelKeys = event.metadata.derivedFromId ?: emptySet()
            scheduledExpiries.filter { it -> it.key in cancelKeys }.keys.forEach { key ->
                scheduledExpiries.remove(key)?.cancel(true)
            }
            return null
        }

        val useEvent = event as? MediaParsedInfoEvent ?: return null

        val task = MetadataSearchTask(
            MetadataSearchTask.SearchData(
                searchTitles = useEvent.data.parsedSearchTitles,
                collection = useEvent.data.parsedCollection
            )
        ).derivedOf(useEvent)
        TaskStore.persist(task)
        val finalResult = MetadataSearchTaskCreatedEvent(task.taskId).derivedOf(useEvent)
        scheduleTaskExpiry(task.taskId, finalResult.eventId, task.referenceId)
        return finalResult
    }

    private fun scheduleTaskExpiry(taskId: UUID, eventId: UUID, referenceId: UUID) {
        if (scheduledExpiries.containsKey(taskId)) return

        val future = scheduler.schedule({
            // Hvis tasken fortsatt ikke har result/failed → marker som failed
            TaskStore.claim(taskId, "Coordinator-MetadataSearchTaskListener-TimeoutScheduler")
            TaskStore.markConsumed(taskId, TaskStatus.Failed)
            val failureEvent = MetadataSearchResultEvent(
                status = TaskStatus.Failed,
            ).apply { setFailed(listOf(taskId)) }
            //publishEvent(MetadataSearchFailedEvent(taskId, "Timeout").derivedOf(referenceId))
            scheduledExpiries.remove(taskId)
        }, 10, TimeUnit.MINUTES)

        scheduledExpiries[taskId] = future
    }

}