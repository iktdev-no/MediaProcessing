package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.MultiTaskCreatorEventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.MultiTaskCreatedEvent
import no.iktdev.eventi.models.MultiTaskIdentity
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.serialization.ZDS.toTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.RevertTransferredRequestedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.RevertTransferContentTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.TransferContentTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.CoverTransferTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.SubtitleTransferTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.VideoTransferTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks_super.TransferTask
import no.iktdev.mediaprocessing.shared.common.getSha256
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RevertTransferContentToStoreTaskCreateListener(): MultiTaskCreatorEventListener(EventStore, TaskStore) {
    override fun isEventOfMyCreation(event: Event) = event is RevertTransferContentTaskCreatedEvent

    override fun onEvent(event: Event, history: List<Event>): Event? {
        event.requireQualifiedEntry<RevertTransferredRequestedEvent>()
        return super.onEvent(event, history)
    }

    override fun onCreateTask(
        event: Event,
        history: List<Event>
    ): List<Task> {
        val taskIds = event.requireQualifiedEntry<RevertTransferredRequestedEvent>().taskIds
        val tasks = taskIds
            .mapNotNull { taskId -> taskStore.findByTaskId(taskId)}
            .mapNotNull { it.toTask() }
            .mapNotNull { reverseTask(event.eventId, it) }
        return tasks
    }


    fun reverseTask(parentId: UUID, task: Task): TransferTask? {
        return when (task) {
            is SubtitleTransferTask -> {
                SubtitleTransferTask(
                    executerId = parentId,
                    collection = task.collection,
                    language = task.language,
                    cachedUri = task.storeUri,
                    storeUri = task.cachedUri
                )
            }
            is CoverTransferTask -> {
                CoverTransferTask(
                    executerId = parentId,
                    collection = task.collection,
                    cachedUri = task.storeUri,
                    storeUri = task.cachedUri
                )
            }
            is VideoTransferTask -> {
                VideoTransferTask(
                    executerId = parentId,
                    collection = task.collection,
                    cachedUri = task.storeUri,
                    storeUri = task.cachedUri
                )
            }

            else -> { null }
        }
    }

    override fun onTasksCreated(
        event: Event,
        history: List<Event>,
        tasks: List<Task>
    ): MultiTaskCreatedEvent {
        return RevertTransferContentTaskCreatedEvent(
            groupId = event.eventId,
            taskIds = tasks.map { MultiTaskIdentity(it.taskId, onGetTaskIdentity(it)) }.toSet()
        )
    }

    override fun onGetTaskIdentity(task: Task): String {
        val key = when (task) {
            is CoverTransferTask -> "${task.collection}::${task.cachedUri}-revert"
            is SubtitleTransferTask -> "${task.collection}::${task.language}::${task.cachedUri}-revert"
            is VideoTransferTask -> "${task.collection}::${task.cachedUri}-revert"
            else -> throw IllegalArgumentException("Unsupported task type: ${task::class.simpleName}")
        }

        return key.getSha256()
    }
}