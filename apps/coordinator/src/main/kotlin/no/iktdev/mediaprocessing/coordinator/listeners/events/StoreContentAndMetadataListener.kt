package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreContentAndMetadataTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreContentAndMetadataTask
import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.projection.StoreProjection
import no.iktdev.mediaprocessing.shared.common.stores.TaskStore
import org.springframework.stereotype.Component

@Component
class StoreContentAndMetadataListener: EventListener() {
    val log = KotlinLogging.logger {}

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event as? MigrateContentToStoreTaskResultEvent ?: return null
        val collectionEvent = history.lastOrNull { it is CollectedEvent } as? CollectedEvent
            ?: return null

        val useHistory = (history.filter { collectionEvent.eventIds.contains(it.eventId) }) + listOf(useEvent)
        val projection = StoreProjection(useHistory)

        val collection = projection.getCollection()
        if (collection.isNullOrBlank()) {
            log.error { "Collection is null @ ${useEvent.referenceId}" }
            return null
        }
        val metadata = projection.projectMetadata()
        if (metadata == null) {
            log.error { "Metadata is null @ ${useEvent.referenceId}"}
            return null
        }


        val exportInfo = ContentExport(
            collection = collection,
            media = projection.projectMediaFiles(),
            episodeInfo = projection.projectEpisodeInfo(),
            metadata = metadata
        )

        val task = StoreContentAndMetadataTask(exportInfo).derivedOf(useEvent)
        TaskStore.persist(task)

        return StoreContentAndMetadataTaskCreatedEvent(task.taskId).derivedOf(useEvent)
    }
}