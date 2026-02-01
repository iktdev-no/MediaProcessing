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
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.processer.CoordinatorClient
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
class VideoTaskListener(
    private var coordinatorWebClient: CoordinatorClient,
    private val localProgress: LocalProgressCache,
    private val executableConfig: ExecutablesConfig,
    private val fileUtil: FileUtil,
) : FfmpegTaskListener(TaskType.CPU_INTENSIVE) {
    private val log = KotlinLogging.logger {}

    override fun getWorkerId() = "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun supports(task: Task) = task is EncodeTask

    override fun accept(task: Task, reporter: TaskReporter): Boolean {
        val accepts = super.accept(task, reporter)
        if (accepts) {
            log.info { "${getWorkerId()} accepts video task ${task.taskId}" }
        }
        return accepts
    }

    override suspend fun onTask(task: Task): Event? {
        val taskData = task as EncodeTask
        val cachedOutFile = fileUtil.getTemporaryStoreFile(taskData.data.outputFileName).also {
            if (!it.parentFile.exists()) {
                it.parentFile.mkdirs()
            }
        }
        if (cachedOutFile.exists() && taskData.data.arguments.firstOrNull() != "-y") {
            reporter?.publishEvent(
                ProcesserEncodeResultEvent(
                    status = TaskStatus.Failed
                ).producedFrom(task)
            )
            throw IllegalStateException("${cachedOutFile.absolutePath} does already exist, and arguments does not permit overwrite")
        }

        val arguments = MpegArgument()
            .inputFile(taskData.data.inputFile)
            .outputFile(cachedOutFile.absolutePath)
            .args(taskData.data.arguments)
            .withProgress(true)

        val logDirectory = fileUtil.getLogDirectory().using("encode")
        val result = getFfmpeg(
            listener = listener,
            logDirectory = logDirectory,
            execPath = executableConfig.ffmpeg
        )
        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }
        result.run(arguments)
        if (result.result.resultCode != 0) {
            return ProcesserEncodeResultEvent(
                status = TaskStatus.Failed,
                logFile = result.logFile.absolutePath
            ).producedFrom(task)
        }

        return ProcesserEncodeResultEvent(
            status = TaskStatus.Completed,
            logFile = result.logFile.absolutePath,
            data = ProcesserEncodeResultEvent.EncodeResult(
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
        return ProcesserEncodeResultEvent(null, null, status, error = message).producedFrom(task)
    }

    val listener = object : FFmpeg.Listener {
        var lastProgress: FfmpegDecodedProgress? = null
        override fun onStarted(inputFile: String) {
        }

        override fun onCompleted(inputFile: String, outputFile: String) {
            currentTask?.let {
                coordinatorWebClient.reportProgress(
                    referenceId = it.referenceId.toString(),
                    taskId = it.taskId.toString(),
                    percent = FfmpegDecodedProgress(
                        100,
                        "",
                        lastProgress?.duration ?: "",
                        "0",
                        estimatedCompletion = "",
                        estimatedCompletionSeconds = 0
                    ),
                    ""
                )
            }
        }

        override fun onProgressChanged(
            inputFile: String,
            progress: FfmpegDecodedProgress
        ) {
            lastProgress = progress
            currentTask?.let {
                localProgress.update(it.taskId, progress)
                coordinatorWebClient.reportProgress(
                    referenceId = it.referenceId.toString(),
                    taskId = it.taskId.toString(),
                    percent = progress,
                    ""
                )
            }

        }
    }

    override fun buildFfmpeg(listener: FFmpeg.Listener?, execPath: String, logDirectory: File): FFmpeg {
        return VideoFFmpeg(execPath = executableConfig.ffmpeg,
            logDirectory = logDirectory,
            listener = listener)
    }

    class VideoFFmpeg(override val listener: Listener? = null, private val execPath: String, val logDirectory: File) :
        FFmpeg(executable = execPath, logDir = logDirectory) {

        override fun onCreate() {
            super.onCreate()
            if (!logDirectory.exists()) {
                logDirectory.mkdirs()
            }
        }
    }
}