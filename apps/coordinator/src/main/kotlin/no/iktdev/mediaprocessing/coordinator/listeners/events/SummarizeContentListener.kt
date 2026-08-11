package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EjectException
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ContinuationSummaryEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DeterminedCollectionTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.getCollection
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.projection.CollectionProjection
import no.iktdev.mediaprocessing.shared.common.projection.SummaryProjection
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.common.short
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
        val useEvent = event.requireQualifiedEntry<CollectedEvent>()
        val useHistory = (history.filter { useEvent.eventIds.contains(it.eventId) })

        val collection = getCollection(history) ?: run {
            log.error { "[${event.referenceId.short()}] Could not find collection for ${event::class.simpleName}!" }
            throw EjectException("Could not find collection for event ${event.eventId}")
        }


        val projection = SummaryProjection(collection, useHistory, coordinatorEnv.outboxFolder)
        val migrationPlan = projection.createMigrationPlan()

        val mediaProjection = projection.projectMediaFiles(migrationPlan)
        val metadata = projection.projectMetadata(migrationPlan)
        if (metadata == null && !canAllowMetadataNull(mediaProjection)) {
            log.error { "[${event.referenceId.short()}] Metadata is null" }
            return null
        }


        val exportInfo = ContentExport(
            collection = collection,
            media = mediaProjection,
            episodeInfo = projection.projectEpisodeInfo(),
            metadata = metadata
        )


        return ContinuationSummaryEvent(
            data = exportInfo,
            plan = migrationPlan,
        ).derivedOf(useEvent)
    }

    fun canAllowMetadataNull(mediaProjection: ContentExport.MediaExport?): Boolean {
        if (mediaProjection == null) return false
        if (mediaProjection.videoFile != null) return false
        if (mediaProjection.subtitles.isNotEmpty()) return true
        return false
    }

}