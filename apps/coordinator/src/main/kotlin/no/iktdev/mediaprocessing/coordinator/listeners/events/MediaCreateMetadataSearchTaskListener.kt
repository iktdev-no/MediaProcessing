package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.ListenerOrder
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.serialization.ZDS.toTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MetadataSearchTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.rejectIfPresent
import no.iktdev.mediaprocessing.shared.common.rejectIfSelf
import no.iktdev.mediaprocessing.shared.common.requireEvent
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
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

    override fun allowDerivativeOnHistoricalEvent() = true

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {

        val startedEvent = history.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return null
        if (startedEvent.data.operation.isNotEmpty()) {
            if (!startedEvent.data.operation.contains(OperationType.MetadataSearch))
                return null
        }

        history.requireEvent<MediaParsedInfoEvent>()
        history.rejectIfPresent<CollectedEvent>()

        // For replay
        if (event is MetadataSearchTaskCreatedEvent) {
            val hasResult = history.filter { it is MetadataSearchResultEvent }
                .any { it.metadata.derivedFromId?.contains(event.taskId) == true }

            if (!hasResult) {
                scheduleTaskExpiry(event.taskId, event.eventId, event.referenceId)
            }
            return null // <- Safeguard sicne we are disabling historically deriviation
        } else if (event is MetadataSearchResultEvent) {
            val cancelKeys = event.metadata.derivedFromId ?: emptySet()
            scheduledExpiries.filter { it -> it.key in cancelKeys }.keys.forEach { key ->
                scheduledExpiries.remove(key)?.cancel(true)
            }
            return null
        }

        val useEvent = history.getInstanceOf<MediaParsedInfoEvent>() ?: return null

        val searchData = MetadataSearchTask.SearchData(
            searchTitles = useEvent.data.parsedSearchTitles,
            collection = useEvent.data.parsedCollection,
            mediaType = useEvent.data.mediaType
        )

        val metadataSearchTask = MetadataSearchTask(data = searchData)
            .derivedOf(useEvent)

        val taskCreatedEvent = MetadataSearchTaskCreatedEvent(taskId = metadataSearchTask.taskId)
            .derivedOf(useEvent).also {
                TaskStore.persist(metadataSearchTask)
                scheduleTaskExpiry(metadataSearchTask.taskId, it.eventId, it.referenceId)
            }


        return taskCreatedEvent
    }

    private fun scheduleTaskExpiry(taskId: UUID, eventId: UUID, referenceId: UUID) {
        if (scheduledExpiries.containsKey(taskId)) return

        val future = scheduler.schedule({
            val task = TaskStore.findByTaskId(taskId)?.toTask() ?: return@schedule
            // Hvis tasken fortsatt ikke har result/failed → marker som failed
            TaskStore.claim(task.taskId, "Coordinator-MetadataSearchTaskListener-TimeoutScheduler")
            TaskStore.markConsumed(task.taskId, TaskStatus.Failed)
            val failureEvent = MetadataSearchResultEvent(
                status = TaskStatus.Failed,
            ).apply { setFailed(listOf(taskId)) }
                .producedFrom(task)
            EventStore.persist(failureEvent)
            scheduledExpiries.remove(taskId)
        }, 10, TimeUnit.MINUTES)

        scheduledExpiries[taskId] = future
    }

}