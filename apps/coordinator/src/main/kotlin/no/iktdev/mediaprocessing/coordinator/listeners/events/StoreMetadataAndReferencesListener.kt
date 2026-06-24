package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ContinuationSummaryEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.PersistContentEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreContentAndMetadataTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.isOnly
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreContentAndMetadataTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.common.projection.TaskProjection
import no.iktdev.mediaprocessing.shared.common.requireEvent
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.springframework.stereotype.Component

@Component
class StoreMetadataAndReferencesListener: EventListener() {
    val log = KotlinLogging.logger {}

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val startEvent = history.requireEvent<StartProcessingEvent>()
        if (startEvent.data.operation.isOnly(OperationType.MetadataSearch)) {
            event.requireQualifiedEntry<PersistContentEvent>()
        }

        if (history.getInstanceOf<PersistContentEvent>() == null) {
            return null
        }
        val transferStatus = TaskProjection(listOf(event) + history)

        if (transferStatus.projectMigrateContentStatus() == CollectProjection.TaskStatus.Failed) {
            return null
        }

        val useEvent = history.getInstanceOf<ContinuationSummaryEvent>() ?: return null


        val task = StoreContentAndMetadataTask(useEvent.data)
        val createdTaskEvent = StoreContentAndMetadataTaskCreatedEvent(task.taskId).derivedOf(event)
        task.apply { derivedOf(createdTaskEvent) }

        TaskStore.persist(task)

        return createdTaskEvent
    }
}