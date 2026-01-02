package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.CoverDownloadTask
import no.iktdev.mediaprocessing.shared.common.stores.TaskStore
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

        val downloadData = useEvent.results.map {
            val data = it.data
            val outputFileName = "${data.title}-${data.source}"
            CoverDownloadTask.CoverDownloadData(
                url = it.data.cover,
                source = it.data.source,
                outputFileName = outputFileName
            )
        }

        val downloadTasks = downloadData.map {
            CoverDownloadTask(it)
                .derivedOf(useEvent)
        }

        downloadTasks.forEach { TaskStore.persist(it) }

        return CoverDownloadTaskCreatedEvent(
            downloadTasks.map { it.taskId }
        )
    }
}