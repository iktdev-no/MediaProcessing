package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.ffmpeg.FFprobe
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoordinatorReadStreamsResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MediaReadTask
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MediaStreamReadTaskListener: FfprobeTaskListener(TaskType.CPU_INTENSIVE) {
    val log = KotlinLogging.logger {}

    override fun getWorkerId(): String {
        return "${this::class.java.simpleName}-${TaskType.CPU_INTENSIVE}-${UUID.randomUUID()}"
    }

    override fun supports(task: Task): Boolean {
        return task is MediaReadTask
    }

    override suspend fun onTask(task: Task): Event? {
        val pickedTask = task as? MediaReadTask ?: return null
        try {
            val probeResult = getFfprobe()
                .readJsonStreams(pickedTask.fileUri)

            val result =
                probeResult.data ?: throw RuntimeException("No data returned from ffprobe for ${pickedTask.fileUri}")

            return CoordinatorReadStreamsResultEvent(
                status = TaskStatus.Completed,
                data = result
            ).producedFrom(task)

        } catch (e: Exception) {
            log.error(e) { "Error reading media streams for ${pickedTask.fileUri}" }
            return CoordinatorReadStreamsResultEvent(
                status = TaskStatus.Failed,
                data = null
            )
        }
    }

    override fun getFfprobe(): FFprobe {
        return JsonFfinfo(CoordinatorEnv.ffprobe)
    }

    class JsonFfinfo(executable: String): FFprobe(executable) {
    }
}