package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EjectException
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.events.MultiTaskCreatorEventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.MultiTaskCreatedEvent
import no.iktdev.eventi.models.MultiTaskIdentity
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.requireAs
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadSkippedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DetermineCollectionTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.CoverDownloadTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.getSha256
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.springframework.stereotype.Component

@Component
class MediaCreateCoverDownloadTaskListener: MultiTaskCreatorEventListener(EventStore, TaskStore) {
    private val log = KotlinLogging.logger {}

    override fun allowDerivativeOnHistoricalEvent() = true

    private val producedEventTypes = listOf(
        CoverDownloadSkippedEvent::class,
        CoverDownloadTaskCreatedEvent::class,
    )

    override fun isEventOfMyCreation(event: Event) = producedEventTypes.any {
        it.isInstance(event)
    }

    override fun onEjectException(event: Event, history: List<Event>, exception: EjectException): Event {
        log.warn(exception.message)
        return when (exception) {
            is SkippedCoverTaskCreation -> CoverDownloadSkippedEvent().derivedOf(event)
            else -> throw exception
        }
    }

    override fun onEvent(event: Event, history: List<Event>): Event? {
        val useEvents = history + event
        if (useEvents.any { it is CompletedEvent }) return null
        return super.onEvent(event, history)
    }

    override fun onCreateTask(
        event: Event,
        history: List<Event>
    ): List<Task> {
        val useEvents = history + event
        if (useEvents.any { it is CompletedEvent }) return emptyList()
        val hasProduces =  producedEventTypes.any { type ->
            useEvents.any { type.isInstance(it) }
        }
        if (hasProduces) return emptyList()

        val useEvent = useEvents.getInstanceOf<MetadataSearchResultEvent>() ?: return emptyList()
        if (useEvent.status != TaskStatus.Completed) {
            log.warn { "MetadataResult on ${event.referenceId} did not complete successfully" }
            throw SkippedCoverTaskCreation("MetadataResult on ${event.referenceId} did not complete successfully")
        }

        val parsedInfo = history.getInstanceOf<MediaParsedInfoEvent>()
            ?.data?.parsedFileName
            ?: run {
                log.error("Unable to get parsing info, thus no output directory to use. Exiting listener")
                throw SkippedCoverTaskCreation("Unable to get parsing info, thus no output directory to use. Exiting listener")
            }

        val downloadData = useEvent.recommended
            ?.metadata
            ?.let { data ->
                data.cover
                    ?.takeIf { it.isNotBlank() }
                    ?.let { cover ->
                        CoverDownloadTask.CoverDownloadData(
                            url = cover,
                            source = data.source,
                            outputFileName = "${data.title}-${data.source}",
                            outputFolderName = parsedInfo
                        )
                    }
            }

        if (downloadData == null) {
            log.info("No cover found for ${event.referenceId}, skipping cover download task creation")
            throw SkippedCoverTaskCreation("No cover found for ${event.referenceId}, skipping cover download task creation")
        }

        val tasks = listOf(CoverDownloadTask(downloadData))
        return tasks
    }

    override fun onTasksCreated(
        event: Event,
        history: List<Event>,
        tasks: List<Task>
    ): MultiTaskCreatedEvent {
        val createdTasksEvent = CoverDownloadTaskCreatedEvent(
            tasks.map { MultiTaskIdentity(it.taskId, onGetTaskIdentity(it)) }.toSet()
        ).derivedOf(event)
        return createdTasksEvent
    }

    override fun onGetTaskIdentity(task: Task): String {
        val t = task.requireAs<CoverDownloadTask>()
        val key = "${t.data.outputFolderName}::${t.data.source}::${t.data.outputFileName}"
        return key.getSha256()
    }

    class SkippedCoverTaskCreation(message: String): EjectException(message)

}