package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
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
import no.iktdev.mediaprocessing.shared.common.requireEventValue
import no.iktdev.mediaprocessing.shared.common.short
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
    val log = KotlinLogging.logger {}

    @VisibleForTesting
    internal val scheduledExpiries = ConcurrentHashMap<UUID, ScheduledFuture<*>>()
    private val scheduler = Executors.newScheduledThreadPool(1)

    override fun allowDerivativeOnHistoricalEvent() = true

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val operations = history.requireEventValue<StartProcessingEvent, Set<OperationType>> { it.data.operation }
        if (operations.isEmpty() || operations.none { it == OperationType.MetadataSearch}) {
            return null
        }

        history.requireEvent<MediaParsedInfoEvent>()
        history.rejectIfPresent<CollectedEvent>()

        val searchResult = history.getInstanceOf<MetadataSearchResultEvent>()
        if (searchResult != null) {
            val cancelKeys = searchResult.metadata.derivedFromId ?: emptySet()
            scheduledExpiries.filter { it -> it.key in cancelKeys }.keys.forEach { key ->
                log.info("[${event.referenceId.short()}] Removing ${event::class.simpleName}, from timeout")
                scheduledExpiries.remove(key)?.cancel(true)
            }
            return null
        }
        val selfCreated = history.getInstanceOf<MetadataSearchTaskCreatedEvent>()
        if (selfCreated != null) {
            log.warn("[${event.referenceId.short()}] Found metadata search event for ${event::class.simpleName}, generating timeout")
            scheduleTaskExpiry(selfCreated.taskId, selfCreated.eventId, selfCreated.referenceId)
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