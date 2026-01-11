package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MigrateToContentStoreTask
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.common.projection.MigrateContentProject
import no.iktdev.mediaprocessing.shared.common.stores.TaskStore
import org.springframework.stereotype.Component

@Component
class MigrateCreateStoreTaskListener(
    private val coordinatorEnv: CoordinatorEnv,
): EventListener() {
    private val log = KotlinLogging.logger {}

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event as? CollectedEvent ?: return null
        val useHistory = history.filter { useEvent.eventIds.contains(it.eventId) }

        val collectProjection = CollectProjection(useHistory)
        log.info { collectProjection.prettyPrint() }

        val statusAcceptable = collectProjection.getTaskStatus().none { it == CollectProjection.TaskStatus.Failed }
        if (!statusAcceptable) {
            log.warn { "One or more tasks have failed in  ${event.referenceId}" }
        }

        val migrateContentProjection = MigrateContentProject(useHistory, coordinatorEnv.outgoingContent)

        val collection = migrateContentProjection.useStore?.name ?:
            throw RuntimeException("No content store configured for migration in ${event.referenceId}")

        val videContent = migrateContentProjection.getVideoStoreFile()?.let { MigrateToContentStoreTask.Data.SingleContent(it.cachedFile.absolutePath, it.storeFile.absolutePath) }
        val subtitleContent = migrateContentProjection.getSubtitleStoreFiles()?.map {
            MigrateToContentStoreTask.Data.SingleSubtitle(it.language, it.cts.cachedFile.absolutePath, it.cts.storeFile.absolutePath, )
        }
        val coverContent = migrateContentProjection.getCoverStoreFiles()?.map {
            MigrateToContentStoreTask.Data.SingleContent(it.cachedFile.absolutePath, it.storeFile.absolutePath)
        }
        val storeTask = MigrateToContentStoreTask(
            MigrateToContentStoreTask.Data(
                collection = collection,
                videoContent = videContent,
                subtitleContent = subtitleContent,
                coverContent = coverContent
            )
        ).derivedOf(event)

        TaskStore.persist(storeTask)

        return MigrateContentToStoreTaskCreatedEvent(storeTask.taskId)
    }
}