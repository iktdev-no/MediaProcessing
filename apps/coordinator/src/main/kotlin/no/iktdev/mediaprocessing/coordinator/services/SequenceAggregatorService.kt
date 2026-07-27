package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.MultiTaskCreatedEvent
import no.iktdev.eventi.models.SingleTaskCratedEvent
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.TaskCratedEvent
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.serialization.WGson
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.eventi.serialization.ZDS.toPersisted
import no.iktdev.mediaprocessing.coordinator.translate
import no.iktdev.mediaprocessing.shared.common.effective
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.common.projection.SignalProjection
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CurrentState
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.LifecycleNode
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.LifecycleNodeType
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.Mode
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceSummary
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.TaskLifecycleItem
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.UiEvent
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.UiTask
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

@Service
class SequenceAggregatorService(
    private val eventService: EventService,
    private val taskService: TaskService,
) {
    fun getActiveSequences(): List<SequenceSummary> {
        val allEvents = EventStore.getPersistedEventsAfter(Instant.EPOCH)
        return getSequenceSummary(allEvents,
            { group: List<PersistedEvent> ->
                group.none { it.event == CompletedEvent::class.java.simpleName }
            }
        )
    }

    fun getRecentSequences(limit: Int): List<SequenceSummary> {
        val allEvents = EventStore.getPersistedEventsAfter(Instant.EPOCH)
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

    private fun Event.toUIEvent(): UiEvent {
        return UiEvent(
            referenceId = this.referenceId,
            eventId = this.eventId,
            event = this::class.simpleName ?: run {
                throw IllegalStateException("Missing class name for event: $this")
            },
            data = WGson.gson.toJson(this),
            persistedAt = this.metadata.created
        )
    }




    /*fun generateLifecycle(refId: UUID): List<LifecycleNode> {
        val lifecycles: MutableList<LifecycleNode> = mutableListOf()

        val events = eventService.getEffectiveHistory(refId).toEvents()
        val tasks = taskService.getTasksByReferenceId(refId)

        val resultEvents = events.filterIsInstance<TaskResultEvent>()
        events.filterIsInstance<TaskCratedEvent>().forEach { createdEvent ->

            val taskIds = when (createdEvent) {
                is SingleTaskCratedEvent -> listOf(createdEvent.taskId)
                is MultiTaskCreatedEvent -> createdEvent.taskIds
                else -> emptyList()
            }

            val createdTasks = tasks.filter { it.taskId in taskIds }.map { task ->
                val relevantResults = resultEvents.filter { task.taskId in (it.metadata.derivedFromId ?: emptySet()) }.map { it.toUIEvent()}
                task to relevantResults
            }

            lifecycles.add(LifecycleNode(
                lifecycleId = createdEvent.eventId,
                referenceId = createdEvent.referenceId,
                type = LifecycleNodeType.EventTaskGroup,
                event = null,
                taskOwnerEvent = createdEvent.toUIEvent(),
                tasks = createdTasks.map { ct ->
                    TaskLifecycleItem(ct.first.taskId, ct.first, ct.second)
                }
            ))

        }

        events.filter { it !is TaskCreatedEvent && it !is TaskResultEvent }.forEach { event ->
            lifecycles.add(LifecycleNode(
                lifecycleId = event.eventId,
                referenceId = event.referenceId,
                type = LifecycleNodeType.Event,
                event = event,
                taskOwnerEvent = null,
            ))
        }

        return lifecycles.sortedBy { node ->
            node.taskOwnerEvent?.persistedAt ?: node.event?.persistedAt
        }
    }*/


}