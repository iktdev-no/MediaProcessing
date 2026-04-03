package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.mediaprocessing.coordinator.translate
import no.iktdev.mediaprocessing.shared.common.effective
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.common.projection.SignalProjection
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CurrentState
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.Mode
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceSummary
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2

@Service
class SequenceAggregatorService(
    private val eventService: EventService
) {
    fun getActiveSequences(): List<SequenceSummary> {
        val allEvents = EventStore.getPersistedEventsAtOrAfter(Instant.EPOCH)
        return getSequenceSummary(allEvents,
            { group: List<PersistedEvent> ->
                group.none { it.event == CompletedEvent::class.java.simpleName }
            }
        )
    }

    fun getRecentSequences(limit: Int): List<SequenceSummary> {
        val allEvents = EventStore.getPersistedEventsAtOrAfter(Instant.EPOCH)
        return getSequenceSummary(allEvents).take(limit)
    }

    fun getSequenceSummary(
        events: List<PersistedEvent>,
        vararg groupFilters: (List<PersistedEvent>) -> Boolean
    ): List<SequenceSummary> {

        val grouped = events
            .groupBy { it.referenceId }
            // filtrer grupper før composeSummary
            .filter { (_, group) -> groupFilters.all { filter -> filter(group) } }
            // bygg summary
            .mapNotNull { (id, group) ->
                composeSummary(group)?.let { summary -> id to summary }
            }
            .toMap()

        val deleted = eventService.getDeletedSequences(grouped.keys)

        return grouped
            .filterNot { (referenceId, _) -> referenceId in deleted }
            .values
            .sortedByDescending { it.lastEventTime }
    }





    fun composeSummary(persisted: List<PersistedEvent>): SequenceSummary? {
        val last = persisted.maxByOrNull { it.persistedAt } ?: return null
        val events = persisted.mapNotNull { it.toEvent() }
        val signals = SignalProjection(events)

        val domainEvents = events.effective()
        val projection = CollectProjection(domainEvents)

        val state = if (signals.isReleased) {
            CurrentState.Continuing
        } else if (signals.isOnHold) {
            CurrentState.OnHold
        } else {
            CurrentState.Continuing
        }

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