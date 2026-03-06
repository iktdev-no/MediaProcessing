package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ContinuationSummaryEvent
import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.model.ContentMigrationPlan
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.common.projection.CollectionProjection
import no.iktdev.mediaprocessing.shared.common.projection.MigrateContentProject
import no.iktdev.mediaprocessing.shared.common.projection.StoreProjection
import org.springframework.stereotype.Component

@Component
class SummarizeContentListener(
    private val coordinatorEnv: CoordinatorEnv,
) : EventListener() {
    val log = KotlinLogging.logger {}

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        if (event !is CollectedEvent) return null

        val useHistory = (history.filter { event.eventIds.contains(it.eventId) })
        val collection = CollectionProjection(useHistory, coordinatorEnv.outboxFolder).getCollection()


        val projection = StoreProjection(useHistory)

        val migrationPlan = createMigrationPlan(collection, useHistory)

        val metadata = projection.projectMetadata()
        if (metadata == null) {
            log.error { "Metadata is null @ ${event.referenceId}" }
            return null
        }


        val exportInfo = ContentExport(
            collection = collection,
            media = projection.projectMediaFiles(),
            episodeInfo = projection.projectEpisodeInfo(),
            metadata = metadata
        )


        return ContinuationSummaryEvent(
            data = exportInfo,
            plan = migrationPlan,
        ).derivedOf(event)
    }

    private fun createMigrationPlan(collection: String, useHistory: List<Event>): ContentMigrationPlan {
        val referenceId = useHistory.first().referenceId
        val collectProjection = CollectProjection(useHistory)
        log.info { collectProjection.prettyPrint() }

        val statusAcceptable = collectProjection.getTaskStatus().none { it == CollectProjection.TaskStatus.Failed }
        if (!statusAcceptable) {
            log.warn { "One or more tasks have failed in sequence referenceId=$referenceId" }
        }

        val migrateContentProjection = MigrateContentProject(collection,useHistory, coordinatorEnv.outboxFolder)

        val collection = migrateContentProjection.useStore?.name
            ?: throw RuntimeException("No content store configured for migration in referenceId=${referenceId}")

        val videContent = migrateContentProjection.getVideoStoreFile()?.let {
            ContentMigrationPlan.SingleContent(
                it.cachedFile.absolutePath,
                it.storeFile.absolutePath
            )
        }
        val subtitleContent = migrateContentProjection.getSubtitleStoreFiles()?.map {
            ContentMigrationPlan.SingleSubtitle(
                it.language,
                it.cts.cachedFile.absolutePath,
                it.cts.storeFile.absolutePath,
            )
        }
        val coverContent = migrateContentProjection.getCoverStoreFiles()?.map {
            ContentMigrationPlan.SingleContent(it.cachedFile.absolutePath, it.storeFile.absolutePath)
        }
        return ContentMigrationPlan(
            collection = collection,
            videoContent = videContent,
            subtitleContent = subtitleContent,
            coverContent = coverContent
        )
    }
}