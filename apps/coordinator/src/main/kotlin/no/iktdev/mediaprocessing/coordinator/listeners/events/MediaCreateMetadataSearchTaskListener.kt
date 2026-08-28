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

    override fun allowDerivativeOnHistoricalEvent() = true

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        history.rejectIfPresent<MetadataSearchTaskCreatedEvent>()

        val operations = history.requireEventValue<StartProcessingEvent, Set<OperationType>> { it.data.operation }
        if (operations.isEmpty() || operations.none { it == OperationType.MetadataSearch}) {
            return null
        }
        history.requireEvent<MediaParsedInfoEvent>()
        history.rejectIfPresent<CollectedEvent>()
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
            }


        return taskCreatedEvent
    }

}