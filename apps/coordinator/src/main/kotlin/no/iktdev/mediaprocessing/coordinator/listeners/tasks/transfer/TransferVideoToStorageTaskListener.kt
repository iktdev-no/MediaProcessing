package no.iktdev.mediaprocessing.coordinator.listeners.tasks.transfer

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.VideoTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.FileCopyProgress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.VideoTransferTask
import org.springframework.stereotype.Service
import java.util.*

@Service
class TransferVideoToStorageTaskListener: TransferToStorageBaseListener() {
    override fun getWorkerId() = "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun createIncompleteStateTaskEvent(
        task: Task,
        status: TaskStatus,
        exception: Exception?
    ): Event {
        return VideoTransferredResultEvent(
            fileUri = null,
            collection = (task as VideoTransferTask).collection,
            status = status,
            error = exception?.message ?: "Unknown error"
        )
    }

    override fun supports(task: Task) = task is VideoTransferTask


    override suspend fun onTask(task: Task): Event? {
        val t = task as VideoTransferTask
        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }
        val overrides = t.overrides ?: emptyList()

        val source = SourceFile(t.cachedUri, t.cachedFileHash)
        val dest = DestinationFile(t.storeUri)

        val success = try {
            transfer(source, dest, overrides) { copy, total ->
                reporter?.updateProgress(
                    t.referenceId,
                    t.taskId,
                    FileCopyProgress(
                        progress = copy.toInt(),
                        source = t.cachedUri,
                        destination = t.storeUri
                    )
                )
            }
            TaskStatus.Completed to null
        } catch (e: FileServiceException) {
            when (e) {
                is FileServiceException.FilesAreIdentical,
                is FileServiceException.SourceAlreadyTransferred -> {
                    TaskStatus.Skipped to e.localizedMessage
                }
                else -> throw e
            }
        }
        catch (e: Exception) {
            TaskStatus.Failed to e.localizedMessage
        }


        return VideoTransferredResultEvent(
            collection = t.collection,
            fileUri = dest.uri,
            status = success.first,
            error = success.second
        )
    }
}