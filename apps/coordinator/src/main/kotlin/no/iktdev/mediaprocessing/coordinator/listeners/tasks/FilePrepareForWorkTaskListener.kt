package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.coordinator.services.DefaultFileSystemService
import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FilePrepareForWorkResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.FileCopyProgress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.FilePrepareForWorkTask
import no.iktdev.streamit.library.db.tables.progress
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.FileSystemException
import java.util.UUID
import kotlin.math.floor

@Component
class FilePrepareForWorkTaskListener: TaskListener(TaskType.IO_INTENSIVE) {
    private val log = KotlinLogging.logger {}

    override fun getWorkerId(): String {
        return "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"
    }

    override fun createIncompleteStateTaskEvent(
        task: Task,
        status: TaskStatus,
        exception: Exception?
    ): Event {
        val message = when (status) {
            TaskStatus.Failed -> exception?.message ?: "Unknown error"
            TaskStatus.Cancelled -> "Canceled"
            else -> ""
        }

        return FilePrepareForWorkResultEvent(
            status = status,
            error = message
        ).producedFrom(task)
    }

    override fun supports(task: Task): Boolean {
        return task is FilePrepareForWorkTask
    }

    override suspend fun onTask(task: Task): Event? {
        val useTask = task as FilePrepareForWorkTask

        val source = File(useTask.data.sourceFile).absoluteFile

        if (!source.exists()) {
            throw FileServiceException.SourceMissing(source)
        }
        val destinationFile = File(useTask.data.destinationFile).absoluteFile

        val fs = getFileSystemService()

        if (!destinationFile.parentFile.exists()) {
            if (!destinationFile.parentFile.mkdirs()) {
                throw FileSystemException("Failed to create directory: ${destinationFile.parent}")
            }
        }


        if (destinationFile.exists()) {
            log.info("${destinationFile.name} already exists, checking if source and existing destination file is identical")
            fs.verifyIdentical(source, destinationFile)

            log.info { "Destination already exists, skipping copy and verifying integrity" }
            return FilePrepareForWorkResultEvent(
                status = TaskStatus.Completed,
                file = destinationFile.absolutePath
            ).producedFrom(useTask)
        }

        var lastProgress = -1

        fs.copyWithProgress(source, destinationFile) { copied, total ->
            val percent = (copied * 100 / total).toInt()

            if (percent > lastProgress) {
                lastProgress = percent
                if (percent == 0 || percent == 50 || percent == 100) {
                    log.info { "Copy progress: $percent%" }
                }
                reporter?.updateProgress(
                    useTask.referenceId,
                    useTask.taskId,
                    FileCopyProgress(
                        progress = percent,
                        source = source.absolutePath,
                        destination = destinationFile.absolutePath
                    )
                )
            }
        }


        log.debug { "Verifying identical: ${source.absolutePath} -> ${destinationFile.absolutePath}" }
        fs.verifyIdentical(source, destinationFile)
        return FilePrepareForWorkResultEvent(
            status = TaskStatus.Completed,
            file = destinationFile.absolutePath
        ).producedFrom(useTask)
    }

    open fun getFileSystemService(): FileSystemService =
        DefaultFileSystemService()



}