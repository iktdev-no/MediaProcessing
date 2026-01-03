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
import no.iktdev.mediaprocessing.shared.common.notExist
import org.springframework.stereotype.Component
import java.util.*

@Component
class DownloadCoverTaskListener: TaskListener(TaskType.MIXED)  {
    val log = KotlinLogging.logger {}

    override fun getWorkerId(): String {
        return "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"
    }

    override fun supports(task: Task): Boolean {
        return task is CoverDownloadTask
    }

    override suspend fun onTask(task: Task): Event? {
        val pickedTask = task as? CoverDownloadTask ?: return null
        log.info { "Downloading cover from ${pickedTask.data.url}" }
        val taskData = pickedTask.data

        val downloadClient = getDownloadClient()
        val downloadResult = try {
            downloadClient.download(taskData.url, taskData.outputFileName)
        } catch (e: Exception) {
            return CoverDownloadResultEvent(status = TaskStatus.Failed)
        }
        val downloadedFile = downloadResult.result

        if (downloadResult.success && downloadedFile != null) {
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

    open fun getDownloadClient(): DownloadClient {
        return DefaultDownloadClient()
    }

    class DefaultDownloadClient() : DownloadClient(
        outDir = CoordinatorEnv.cachedContent,
        connectionFactory = DefaultConnectionFactory(),) {
        override fun onCreate() {
            super.onCreate()
            if (outDir.notExist()) {
                outDir.mkdirs()
            }
        }
    }


}