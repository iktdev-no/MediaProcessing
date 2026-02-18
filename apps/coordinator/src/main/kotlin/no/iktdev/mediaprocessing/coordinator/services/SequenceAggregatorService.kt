package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.mediaprocessing.coordinator.translate
import no.iktdev.mediaprocessing.shared.common.effective
import no.iktdev.mediaprocessing.shared.common.effectivePersisted
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CurrentState
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.Mode
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceSummary
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SequenceAggregatorService(
    private val eventService: EventService
) {

    fun getActiveSequences(): List<SequenceSummary> {
        val allEvents = EventStore.getPersistedEventsAfter(Instant.EPOCH)

        // Gruppér først, deserialiser senere
        val grouped = allEvents
                .groupBy { it.referenceId }
                .mapValues { (_, events) -> events.effectivePersisted() }

        val deleted = eventService.getDeletedSequences(grouped.keys)

        return grouped
            .filterNot { (referenceId, _) -> referenceId in deleted }
            .values
            // aktive = ingen CollectedEvent
            .filter { events -> events.none { it.event == CompletedEvent::class.java.simpleName } }
            .mapNotNull { events -> buildSummary(events) }
            .sortedByDescending { it.lastEventTime }
    }

    fun getRecentSequences(limit: Int): List<SequenceSummary> {
        val allEvents = EventStore.getPersistedEventsAfter(Instant.EPOCH)

        val grouped = allEvents.groupBy { it.referenceId }
            .mapValues { (_, events) -> events.effectivePersisted() }
        val deleted = eventService.getDeletedSequences(grouped.keys)

        return grouped
            .filterNot { (referenceId, _) -> referenceId in deleted }
            .values
            .mapNotNull { events -> buildSummary(events) }
            .sortedByDescending { it.lastEventTime }
            .take(limit)
    }

    private fun buildSummary(events: List<PersistedEvent>): SequenceSummary? {
        val last = events.maxByOrNull { it.persistedAt } ?: return null

        // Deserialiser kun eventene for denne sekvensen
        val domainEvents = events.mapNotNull { it.toEvent() }
            .effective()

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
            readStreamsTaskStatus = projection.readStreamsTaskStatus.translate(),
            metadataTaskStatus = projection.metadataTaskStatus.translate(),
            encodeTaskStatus = projection.encodeTaskStatus.translate(),
            extractTaskStatus = projection.extreactTaskStatus.translate(),
            convertTaskStatus = projection.convertTaskStatus.translate(),
            coverDownloadTaskStatus = projection.coverDownloadTaskStatus.translate(),
            contentMigratedTaskStatus = projection.contentMigratedTaskStatus.translate(),
            contentStoredTaskStatus = projection.contentStoredTaskStatus.translate(),
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