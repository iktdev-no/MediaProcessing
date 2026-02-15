package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.CoverDownloadTask
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

        val downloadData = useEvent.results
            .mapNotNull { result ->
                val cover = result.metadata.cover ?: return@mapNotNull null
                val data = result.metadata

                CoverDownloadTask.CoverDownloadData(
                    url = cover,
                    source = data.source,
                    outputFileName = "${data.title}-${data.source}"
                )
            }
            .distinctBy { it.url }




        val tasks = downloadData.map {
            CoverDownloadTask(it)
        }

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