package no.iktdev.mediaprocessing.coordinator.listeners.tasks.transfer

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.CoverTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.VideoTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.CoverTransferTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.VideoTransferTask
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TransferCoverToStorageTaskListener: TransferToStorageBaseListener(deleteSourceAfterVerify = false) {
    override fun getWorkerId() = "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun createIncompleteStateTaskEvent(
        task: Task,
        status: TaskStatus,
        exception: Exception?
    ): Event {
        return CoverTransferredResultEvent(
            fileUri = null,
            collection = (task as CoverTransferTask).collection,
            status = status,
            error = exception?.message ?: "Unknown error"
        ).producedFrom(task)
    }

    override fun supports(task: Task) = task is CoverTransferTask


    override suspend fun onTask(task: Task): Event? {
        val t = task as CoverTransferTask
        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }
        val overrides = t.overrides ?: emptyList()

        val source = SourceFile(t.cachedUri)
        val dest = DestinationFile(t.storeUri)

        val success = try {
            transfer(source, dest, overrides)
            TaskStatus.Completed to null
        } catch (e: FileServiceException) {
            when (e) {
                is FileServiceException.FilesAreIdentical,
                is FileServiceException.SourceAlreadyTransferred -> {
                    TaskStatus.Skipped to e.localizedMessage
                }
                else -> throw e
            }
        } catch (e: Exception) {
            TaskStatus.Failed to e.localizedMessage
        }


        return CoverTransferredResultEvent(
            collection = t.collection,
            fileUri = dest.uri,
            status = success.first,
            error = success.second
        ).producedFrom(task)
    }
}