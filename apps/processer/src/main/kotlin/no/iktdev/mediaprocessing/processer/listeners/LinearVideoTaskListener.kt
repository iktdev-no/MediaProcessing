package no.iktdev.mediaprocessing.processer.listeners

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.processer.CoordinatorClient
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.EncodeProgress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.LinearEncodeTask
import org.springframework.stereotype.Service
import java.util.*

@Service
class LinearVideoTaskListener(
    private var coordinatorWebClient: CoordinatorClient,
    private val localProgress: LocalProgressCache,
    private val executableConfig: ExecutablesConfig,
    private val fileUtil: FileUtil,
    private val processerProperties: ProcesserProperties
) : VideoTaskListener(TaskType.CPU_INTENSIVE, executableConfig) {
    private val log = KotlinLogging.logger {}


    override fun getWorkerId() = "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"
    override fun supports(task: Task): Boolean =
        task is LinearEncodeTask

    override fun accept(task: Task, reporter: TaskReporter): Boolean {
        val accepts = super.accept(task, reporter)
        if (accepts) {
            log.info { "${getWorkerId()} accepts video task ${task.taskId}" }
        }
        return accepts
    }

    override suspend fun onTask(task: Task): Event? {
        val taskData = task as LinearEncodeTask

        val cacheOutputFolder = fileUtil.getTemporaryStoreFolder(taskData.data.outputFolderName)
            .also { if (!it.exists()) {
                it.mkdirs()
            }
            }

        val dsl = ffmpeg {
            fromInstructions(taskData.data.instructions)
            outputDirectory(cacheOutputFolder)
        }

        val cachedOutFile = cacheOutputFolder.using(taskData.data.outputFileName)

        if (cachedOutFile.exists() && !dsl.overwrite()) {
            reporter?.publishEvent(
                ProcesserEncodeResultEvent(
                    status = TaskStatus.Failed
                ).producedFrom(task)
            )
            throw IllegalStateException("${cachedOutFile.absolutePath} does already exist, and arguments does not permit overwrite")
        }

        val logDirectory = fileUtil.getLogDirectory().using("encode_linear")
        val result = getFfmpeg(
            listener = listener,
            logDirectory = logDirectory,
        )
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

        return ProcesserEncodeResultEvent(
            status = TaskStatus.Completed,
            logFile = result.logFile.absolutePath,
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = cachedOutFile.absolutePath
            )
        ).producedFrom(task)
    }

    val listener = object : FFmpeg.Listener {
        var lastProgress: FfmpegDecodedProgress? = null
        override fun onStarted(inputFile: String) {
        }

        override fun onCompleted(inputFile: String, outputFile: String) {
            currentTask?.let {
                val progress = EncodeProgress(
                    progress = 100,
                    ffmpegDecodedProgress = FfmpegDecodedProgress(
                        100,
                        "",
                        lastProgress?.duration ?: "",
                        "0",
                        estimatedCompletion = "",
                        estimatedCompletionSeconds = 0
                    ),
                    ""
                )
                reporter?.updateProgress(it.referenceId, it.taskId, progress)
            }
        }

        override fun onProgressChanged(
            inputFile: String,
            progress: FfmpegDecodedProgress
        ) {
            lastProgress = progress
            currentTask?.let {
                val progress = EncodeProgress(
                    progress = progress.progress,
                    ffmpegDecodedProgress = progress,
                    ""
                )
                reporter?.updateProgress(it.referenceId, it.taskId, progress)
            }

        }
    }


}