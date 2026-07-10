package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.SingleTaskCreatorEventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.SingleTaskCratedEvent
import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ContinuationSummaryEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.PersistContentEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreMediaInfoAndMetadataTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreMediaInfoAndMetadataTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.isOnly
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events_super.TransferredBaseResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreMediaInfoAndMetadataTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.common.projection.TaskProjection
import no.iktdev.mediaprocessing.shared.common.requireEvent
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.springframework.stereotype.Component

@Component
class StoreMetadataAndReferencesListener: SingleTaskCreatorEventListener(eventStore = EventStore, taskStore = TaskStore) {
    val log = KotlinLogging.logger {}

    override fun isEventOfMyCreation(event: Event) = event is StoreMediaInfoAndMetadataTaskCreatedEvent

    val requiredTransferStatus = listOf(
        CollectProjection.TaskStatus.Skipped,
        CollectProjection.TaskStatus.Completed
    )

    override fun onCreateTask(
        event: Event,
        history: List<Event>
    ): Task? {
        val startEvent = history.requireEvent<StartProcessingEvent>()
        if (startEvent.data.operation.isOnly(OperationType.MetadataSearch)) {
            event.requireQualifiedEntry<PersistContentEvent>()
        } else {
            event.requireQualifiedEntry<TransferredBaseResultEvent>()
        }

        if (history.getInstanceOf<PersistContentEvent>() == null) {
            return null
        }
        val transferStatus = TaskProjection(listOf(event) + history)

        if (transferStatus.projectMigrateContentStatus() !in requiredTransferStatus) {
            return null
        }

        val useEvent = history.getInstanceOf<ContinuationSummaryEvent>() ?: return null
        return StoreMediaInfoAndMetadataTask(useEvent.data)
    }

    override fun onTaskCreated(
        event: Event,
        history: List<Event>,
        task: Task
    ): SingleTaskCratedEvent {

        return StoreMediaInfoAndMetadataTaskCreatedEvent(task.taskId)
            .derivedOf(event)
    }
}