package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.requireAs
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadSkippedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DetermineCollectionTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.CoverDownloadTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.springframework.stereotype.Component

@Component
class MediaCreateCoverDownloadTaskListener: EventListener() {
    private val log = KotlinLogging.logger {}

    override fun allowDerivativeOnHistoricalEvent() = true

    private val producedEventTypes = listOf(
        CoverDownloadSkippedEvent::class,
        CoverDownloadTaskCreatedEvent::class,
    )

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvents = history + event
        if (useEvents.any { it is CompletedEvent }) return null
        val hasProduces =  producedEventTypes.any { type ->
            useEvents.any { type.isInstance(it) }
        }
        if (hasProduces) return null

        val useEvent = useEvents.getInstanceOf<MetadataSearchResultEvent>() ?: return null
        if (useEvent.status != TaskStatus.Completed) {
            log.warn("MetadataResult on ${event.referenceId} did not complete successfully")
            return null
        }

        val parsedInfo = history.getInstanceOf<MediaParsedInfoEvent>()
            ?.data?.parsedFileName
            ?: run {
                log.error("Unable to get parsing info, thus no output directory to use. Exiting listener")
                return null
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
            return null
        }

        val tasks = listOf(CoverDownloadTask(downloadData))

        val createdTasksEvent = CoverDownloadTaskCreatedEvent(
            tasks.map { it.taskId }
        ).derivedOf(event)

        tasks.forEach { task ->
            task.apply { derivedOf(createdTasksEvent) }
            TaskStore.persist(task)
        }

        return createdTasksEvent
    }

}