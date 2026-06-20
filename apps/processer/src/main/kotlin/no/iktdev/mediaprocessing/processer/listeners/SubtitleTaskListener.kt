package no.iktdev.mediaprocessing.processer.listeners

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.eventi.tasks.TaskValidator
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import org.springframework.stereotype.Service
import java.util.*

@Service
class SubtitleTaskListener(
    private val executableConfig: ExecutablesConfig,
    private val fileUtil: FileUtil
) : FfTaskListener(TaskType.CPU_INTENSIVE, executableConfig) {
    private val log = KotlinLogging.logger {}


    override fun getWorkerId() = "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun supports(task: Task) = task is ExtractSubtitleTask

    override fun accept(task: Task, reporter: TaskReporter, validator: TaskValidator?): Boolean {
        val accepts = super.accept(task, reporter,validator)
        if (accepts) {
            log.info { "${getWorkerId()} accepts subtitle task ${task.taskId}" }
        }
        return accepts
    }

    override suspend fun onTask(task: Task): Event? {
        val taskData = task as ExtractSubtitleTask

        val cacheOutputFolder = fileUtil.getTemporaryStoreFolder(taskData.data.outputFolderName).using("subtitles", taskData.data.language)
            .apply {
                if (!this.exists()) {
                    mkdirs()
                }
            }

        val dsl = ffmpeg {
            fromInstructions(taskData.data.instructions)
            outputDirectory(cacheOutputFolder)
        }

        val cachedOutFile = cacheOutputFolder.using(taskData.data.outputFileName)

        if (cachedOutFile.exists() && !dsl.overwrite()) {
            reporter?.publishEvent(
                ProcesserExtractResultEvent(
                    status = TaskStatus.Failed
                ).producedFrom(task)
            )
            throw IllegalStateException("${cachedOutFile.absolutePath} does already exist, and arguments does not permit overwrite")
        }


        val logDirectory = fileUtil.getLogDirectory().using("subtitles")
        val result = getFfmpeg(logDirectory = logDirectory)
        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }
        result.run(dsl)
        if (result.result.resultCode != 0) {
            throw FfmpegFailedException(
                logFile = result.logFile,
                "FFmpeg worker returned non zero result code, was ${result.result.resultCode}"
            )
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
        val logFile = if (exception is FfmpegFailedException) exception.logFile?.absolutePath else null
        return ProcesserExtractResultEvent(null, status, error = message, logFile = logFile).producedFrom(task)
    }

    override fun buildFfmpeg(listener: FFmpeg.Listener?, execPath: String, logDirectory: IFile): FFmpeg {
        return SubtitleFFmpeg(listener, executableConfig.ffmpeg, logDirectory)
    }

    class SubtitleFFmpeg(override val listener: Listener? = null, private val executablePath: String, val logDirectory: IFile) :
        FFmpeg(executable = executablePath, logDir = logDirectory) {

        override fun onCreate() {
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
        }
    }
}