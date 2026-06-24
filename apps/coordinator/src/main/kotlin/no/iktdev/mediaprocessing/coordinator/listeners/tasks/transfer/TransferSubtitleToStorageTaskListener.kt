package no.iktdev.mediaprocessing.coordinator.listeners.tasks.transfer

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.CoverTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.SubtitleTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.VideoTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.CoverTransferTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.SubtitleTransferTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.VideoTransferTask
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TransferSubtitleToStorageTaskListener: TransferToStorageBaseListener() {
    override fun getWorkerId() = "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun createIncompleteStateTaskEvent(
        task: Task,
        status: TaskStatus,
        exception: Exception?
    ): Event {
        return SubtitleTransferredResultEvent(
            fileUri = null,
            collection = (task as SubtitleTransferTask).collection,
            language = task.language,
            status = status,
            error = exception?.message ?: "Unknown error"
        )
    }

    override fun supports(task: Task) = task is SubtitleTransferTask


    override suspend fun onTask(task: Task): Event? {
        val t = task as SubtitleTransferTask
        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }
        val overrides = t.overrides ?: emptyList()

        val source = SourceFile(t.cachedUri)
        val dest = DestinationFile(t.storeUri)

        val success = try {
            transfer(source, dest, overrides)
            TaskStatus.Completed to null
        } catch (e: FileServiceException.FilesAreIdentical) {
            TaskStatus.Skipped to e.localizedMessage
        } catch (e: Exception) {
            TaskStatus.Failed to e.localizedMessage
        }


        return SubtitleTransferredResultEvent(
            collection = t.collection,
            fileUri = dest.uri,
            language = t.language,
            status = success.first,
            error = success.second
        )
    }
}