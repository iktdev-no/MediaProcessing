package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.ZDS.toEvent
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.mediaprocessing.shared.common.dto.CurrentState
import no.iktdev.mediaprocessing.shared.common.dto.Mode
import no.iktdev.mediaprocessing.shared.common.dto.SequenceSummary
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SequenceAggregatorService() {

    fun getActiveSequences(): List<SequenceSummary> {
        val allEvents = EventStore.getPersistedEventsAfter(Instant.EPOCH)

        // Gruppér først, deserialiser senere
        val grouped = allEvents.groupBy { it.referenceId }

        return grouped.values
            // aktive = ingen CollectedEvent
            .filter { events -> events.none { it.event == CollectedEvent::class.java.simpleName } }
            .mapNotNull { events -> buildSummary(events) }
            .sortedByDescending { it.lastEventTime }
    }

    fun getRecentSequences(limit: Int): List<SequenceSummary> {
        val allEvents = EventStore.getPersistedEventsAfter(Instant.EPOCH)

        val grouped = allEvents.groupBy { it.referenceId }

        return grouped.values
            .mapNotNull { events -> buildSummary(events) }
            .sortedByDescending { it.lastEventTime }
            .take(limit)
    }

    private fun buildSummary(events: List<PersistedEvent>): SequenceSummary? {
        val last = events.maxByOrNull { it.persistedAt } ?: return null

        // Deserialiser kun eventene for denne sekvensen
        val domainEvents = events.mapNotNull { it.toEvent() }

        val projection = CollectProjection(domainEvents)

        val state = if (events.any { it.event == CollectedEvent::class.java.simpleName }) {
            if (projection.isStorePermitted()) CurrentState.Continuing else CurrentState.OnHold
        } else CurrentState.Continuing

        return SequenceSummary(
            referenceId = last.referenceId.toString(),
            title = "",
            inputFileName = projection.useFile?.name,
            lastEventId = last.eventId.toString(),
            lastEventTime = last.persistedAt,
            metadataTaskStatus = projection.metadataTaskStatus,
            encodeTaskStatus = projection.encodeTaskStatus,
            extractTaskStatus = projection.extreactTaskStatus,
            convertTaskStatus = projection.convertTaskStatus,
            coverDownloadTaskStatus = projection.coverDownloadTaskStatus,
            mode = when (projection.startedWith?.mode) {
                StartFlow.Auto -> Mode.Auto
                StartFlow.Manual -> Mode.Manual
                else -> Mode.Auto
            },
            currentState = state,
            hasErrors = projection.getTaskStatus().any { it == CollectProjection.TaskStatus.Failed }
        )
    }
}