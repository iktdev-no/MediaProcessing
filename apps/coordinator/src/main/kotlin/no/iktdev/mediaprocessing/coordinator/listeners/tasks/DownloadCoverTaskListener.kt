package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.shared.common.DownloadClient
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.CoverDownloadTask
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DownloadCoverTaskListener: TaskListener(TaskType.MIXED)  {
    val log = KotlinLogging.logger {}

    override fun getWorkerId(): String {
        return "${this::class.java.simpleName}-${TaskType.CPU_INTENSIVE}-${UUID.randomUUID()}"
    }

    override fun supports(task: Task): Boolean {
        return task is CoverDownloadTask
    }

    override suspend fun onTask(task: Task): Event? {
        val pickedTask = task as? CoverDownloadTask ?: return null
        log.info { "Downloading cover from ${pickedTask.data.url}" }
        val taskData = pickedTask.data

        val downloadClient = DownloadClient(taskData.url, CoordinatorEnv.cachedContent, taskData.outputFileName)
        val downloadedFile = downloadClient.download()


        if (downloadedFile?.exists() == true) {
            log.info { "Downloaded cover to ${downloadedFile.absolutePath}" }
            return CoverDownloadResultEvent(
                status = TaskStatus.Completed,
                data = CoverDownloadResultEvent.CoverDownloadedData(
                    source = taskData.source,
                    outputFile = downloadedFile.absolutePath
                )
            ).producedFrom(pickedTask)
        } else {
            log.error { "Failed to download cover from ${taskData.url}" }
            return CoverDownloadResultEvent(
                status = TaskStatus.Failed,
            )
        }
    }


}