package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.CoverDownloadTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.springframework.stereotype.Component

@Component
class MediaCreateCoverDownloadTaskListener: EventListener() {
    private val log = KotlinLogging.logger {}

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event as? MetadataSearchResultEvent ?: return null
        if (useEvent.status != TaskStatus.Completed) {
            log.warn("MetadataResult on ${event.referenceId} did not complete successfully")
            return null
        }

        val parsedInfo = history.getInstanceOf<MediaParsedInfoEvent>()?.data?.parsedFileName

        val downloadData = useEvent.recommended?.let { recommended ->
            val cover = recommended.metadata.cover ?: return@let null
            val data = recommended.metadata

            CoverDownloadTask.CoverDownloadData(
                url = cover,
                source = data.source,
                outputFileName = "${data.title}-${data.source}",
                outputFolderName = parsedInfo
            )
        }
        if (downloadData == null) {
            log.info("No cover found for ${event.referenceId}, skipping cover download task creation")
            return null
        }

        val tasks = listOf(
            CoverDownloadTask(downloadData)
        )

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