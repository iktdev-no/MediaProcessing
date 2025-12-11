package no.iktdev.mediaprocessing.processer.listeners

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.arguments.MpegArgument
import no.iktdev.mediaprocessing.processer.ProcesserEnv
import no.iktdev.mediaprocessing.processer.Util
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ExtractResult
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SubtitleTaskListener: TaskListener(TaskType.CPU_INTENSIVE) {
    override fun getWorkerId() = "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun supports(task: Task) = task is ExtractSubtitleTask

    override suspend fun onTask(task: Task): Event? {
        val taskData = task as ExtractSubtitleTask

        val cachedOutFile = Util.getTemporaryStoreFile(taskData.data.outputFileName).also {
            if (!it.parentFile.exists()) {
                it.parentFile.mkdirs()
            }
        }

        if (cachedOutFile.exists() && taskData.data.arguments.firstOrNull() != "-y") {
            reporter?.publishEvent(ProcesserExtractEvent(
                data = ExtractResult(
                    status = TaskStatus.Failed
                )
            ).producedFrom(task))
            throw IllegalStateException("${cachedOutFile.absolutePath} does already exist, and arguments does not permit overwrite")
        }

        val arguments = MpegArgument()
            .inputFile(taskData.data.inputFile)
            .outputFile(cachedOutFile.absolutePath)
            .args(taskData.data.arguments)

        val result = SubtitleFFmpeg()
        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }
        result.run(arguments)
        if (result.result.resultCode != 0 ) {
            return ProcesserExtractEvent(data = ExtractResult(status = TaskStatus.Failed)).producedFrom(task)
        }

        return ProcesserExtractEvent(
            data = ExtractResult(
                status = TaskStatus.Completed,
                cachedOutputFile = cachedOutFile.absolutePath
            )
        ).producedFrom(task)
    }



    class SubtitleFFmpeg(override val listener: Listener? = null): FFmpeg(executable = ProcesserEnv.ffmpeg, logDir = ProcesserEnv.subtitleExtractLogDirectory ) {

        override fun onCreate() {
            if (!ProcesserEnv.subtitleExtractLogDirectory.exists()) {
                ProcesserEnv.subtitleExtractLogDirectory.mkdirs()
            }
        }
    }
}