package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ContinuationSummaryEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.PersistContentEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MigrateToContentStoreTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component

@Component
class MigrateCreateStoreTaskCreateListener(
    private val coordinatorEnv: CoordinatorEnv,
): EventListener() {
    private val log = KotlinLogging.logger {}

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {

        (history + listOf(event)).getInstanceOf<PersistContentEvent>() ?: return null
        val useEvent = history.getInstanceOf<ContinuationSummaryEvent>() ?: return null

        val storeTask = MigrateToContentStoreTask(useEvent.plan)
        val createdTaskEvent = MigrateContentToStoreTaskCreatedEvent(storeTask.taskId).derivedOf(useEvent)
        storeTask.apply { derivedOf(createdTaskEvent) }

        TaskStore.persist(storeTask)

        return createdTaskEvent
    }
}