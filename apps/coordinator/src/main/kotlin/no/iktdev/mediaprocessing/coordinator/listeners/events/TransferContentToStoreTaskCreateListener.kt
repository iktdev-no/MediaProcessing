package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.MultiTaskCreatorEventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.MultiTaskCreatedEvent
import no.iktdev.eventi.models.MultiTaskIdentity
import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ContinuationSummaryEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.PersistContentEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.TransferContentTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.CoverTransferTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.SubtitleTransferTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks_super.TransferTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.VideoTransferTask
import no.iktdev.mediaprocessing.shared.common.getSha256
import no.iktdev.mediaprocessing.shared.common.requireEvent
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component

@Component
class TransferContentToStoreTaskCreateListener():
    MultiTaskCreatorEventListener(EventStore, TaskStore) {
    private val log = KotlinLogging.logger {}

    override fun isEventOfMyCreation(event: Event) = event is TransferContentTaskCreatedEvent

    override fun onEvent(event: Event, history: List<Event>): Event? {
        event.requireQualifiedEntry<PersistContentEvent>()
        return super.onEvent(event, history)
    }

    override fun onCreateTask(
        event: Event,
        history: List<Event>
    ): List<Task> {
        event.requireQualifiedEntry<PersistContentEvent>()
        val useEvent = history.requireEvent<ContinuationSummaryEvent>()
        val plan = useEvent.plan

        val tasks = mutableListOf<TransferTask>()
        plan.videoContent?.let { vc ->
            VideoTransferTask(
                executerId = event.eventId,
                collection = plan.collection,
                storeUri = vc.storeUri,
                cachedUri = vc.cachedUri,
            )
        }?.let { tasks.add(it) }
        plan.coverContent?.let { cc ->
            CoverTransferTask(
                executerId = event.eventId,
                collection = plan.collection,
                storeUri = cc.storeUri,
                cachedUri = cc.cachedUri,
            )
        }?.let { tasks.add(it) }
        plan.subtitleContent?.forEach { sc ->
            SubtitleTransferTask(
                executerId = event.eventId,
                collection = plan.collection,
                storeUri = sc.storeUri,
                cachedUri = sc.cachedUri,
                language = sc.language,
            ).let { tasks.add(it) }
        }

        return tasks
    }

    override fun onTasksCreated(
        event: Event,
        history: List<Event>,
        tasks: List<Task>
    ): MultiTaskCreatedEvent {
        return TransferContentTaskCreatedEvent(
            groupId = event.eventId,
            taskIds = tasks.map { MultiTaskIdentity(it.taskId, onGetTaskIdentity(it)) }.toSet()
        )
    }

    override fun onGetTaskIdentity(task: Task): String {
        val key = when (task) {
            is CoverTransferTask -> "${task.collection}::${task.storeUri}"
            is SubtitleTransferTask -> "${task.collection}::${task.language}::${task.storeUri}"
            is VideoTransferTask -> "${task.collection}::${task.storeUri}"
            else -> throw IllegalArgumentException("Unsupported task type: ${task::class.simpleName}")
        }

        return key.getSha256()
    }
}