package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ContinuationSummaryEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.PersistContentEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreContentAndMetadataTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.isOnly
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreContentAndMetadataTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.model.MigrateStatus
import no.iktdev.mediaprocessing.shared.common.requireEvent
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.springframework.stereotype.Component

@Component
class StoreContentAndMetadataListener: EventListener() {
    val log = KotlinLogging.logger {}

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val startEvent = history.requireEvent<StartProcessingEvent>()
        if (startEvent.data.operation.isOnly(OperationType.MetadataSearch)) {
            event.requireQualifiedEntry<PersistContentEvent>()
        } else {
            history.requireEvent<PersistContentEvent>()
            val migrateEvent = event as? MigrateContentToStoreTaskResultEvent ?: return null
            if (migrateEvent.status == TaskStatus.Failed) {
                return null
            }
        }

        val useEvent = history.getInstanceOf<ContinuationSummaryEvent>() ?: return null


        val task = StoreContentAndMetadataTask(useEvent.data)
        val createdTaskEvent = StoreContentAndMetadataTaskCreatedEvent(task.taskId).derivedOf(useEvent)
        task.apply { derivedOf(createdTaskEvent) }

        TaskStore.persist(task)

        return createdTaskEvent
    }
}