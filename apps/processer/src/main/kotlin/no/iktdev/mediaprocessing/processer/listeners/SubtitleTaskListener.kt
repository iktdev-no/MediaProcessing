package no.iktdev.mediaprocessing.processer.listeners

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.arguments.MpegArgument
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
class SubtitleTaskListener(
    private val executableConfig: ExecutablesConfig,
    private val fileUtil: FileUtil
) : FfmpegTaskListener(TaskType.CPU_INTENSIVE) {
    private val log = KotlinLogging.logger {}


    override fun getWorkerId() = "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun supports(task: Task) = task is ExtractSubtitleTask

    override fun accept(task: Task, reporter: TaskReporter): Boolean {
        val accepts = super.accept(task, reporter)
        if (accepts) {
            log.info { "${getWorkerId()} accepts subtitle task ${task.taskId}" }
        }
        return accepts
    }

    override suspend fun onTask(task: Task): Event? {
        val taskData = task as ExtractSubtitleTask

        val cachedOutFile = fileUtil.getTemporaryStoreFile(taskData.data.outputFileName).also {
            if (!it.parentFile.exists()) {
                it.parentFile.mkdirs()
            }
        }

        if (cachedOutFile.exists() && taskData.data.arguments.firstOrNull() != "-y") {
            reporter?.publishEvent(
                ProcesserExtractResultEvent(
                    status = TaskStatus.Failed
                ).producedFrom(task)
            )
            throw IllegalStateException("${cachedOutFile.absolutePath} does already exist, and arguments does not permit overwrite")
        }

        val arguments = MpegArgument()
            .inputFile(taskData.data.inputFile)
            .outputFile(cachedOutFile.absolutePath)
            .args(taskData.data.arguments)

        val result = getFfmpeg()
        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }
        result.run(arguments)
        if (result.result.resultCode != 0) {
            return ProcesserExtractResultEvent(status = TaskStatus.Failed).producedFrom(task)
        }

        return ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                language = taskData.data.language,
                cachedOutputFile = cachedOutFile.absolutePath
            )
        ).producedFrom(task)
    }

    override fun createIncompleteStateTaskEvent(
        task: Task,
        status: TaskStatus,
        exception: Exception?
    ): Event {
        val message = when (status) {
            TaskStatus.Failed -> exception?.message ?: "Unknown error, see log"
            TaskStatus.Cancelled -> "Canceled"
            else -> ""
        }
        return ProcesserExtractResultEvent(null, status, error = message).producedFrom(task)
    }

    override fun getFfmpeg(): FFmpeg {
        val logDirectory = fileUtil.getLogDirectory().using("subtitles")
        return SubtitleFFmpeg(null, executableConfig.ffmpeg, logDirectory)
    }


    class SubtitleFFmpeg(override val listener: Listener? = null, private val executablePath: String, val logDirectory: File) :
        FFmpeg(executable = executablePath, logDir = logDirectory) {

        override fun onCreate() {
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
        }
    }
}