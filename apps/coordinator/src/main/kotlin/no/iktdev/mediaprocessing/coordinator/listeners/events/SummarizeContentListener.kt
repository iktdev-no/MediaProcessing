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
import no.iktdev.mediaprocessing.shared.common.projection.SummaryProjection
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

        val projection = SummaryProjection(collection, useHistory, coordinatorEnv.outboxFolder)
        val migrationPlan = projection.createMigrationPlan()


        val metadata = projection.projectMetadata(migrationPlan)
        if (metadata == null) {
            log.error { "Metadata is null @ ${event.referenceId}" }
            return null
        }


        val exportInfo = ContentExport(
            collection = collection,
            media = projection.projectMediaFiles(migrationPlan),
            episodeInfo = projection.projectEpisodeInfo(),
            metadata = metadata
        )


        return ContinuationSummaryEvent(
            data = exportInfo,
            plan = migrationPlan,
        ).derivedOf(event)
    }
}