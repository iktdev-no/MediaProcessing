package no.iktdev.mediaprocessing.processer.listeners

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.arguments.MpegArgument
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.processer.CoordinatorClient
import no.iktdev.mediaprocessing.processer.ProcesserEnv
import no.iktdev.mediaprocessing.processer.Util
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import org.springframework.stereotype.Service
import java.util.*

@Service
class VideoTaskListener(private var coordinatorWebClient: CoordinatorClient): FfmpegTaskListener(TaskType.CPU_INTENSIVE) {
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
        val cachedOutFile = Util.getTemporaryStoreFile(taskData.data.outputFileName).also {
            if (!it.parentFile.exists()) {
                it.parentFile.mkdirs()
            }
        }
        if (cachedOutFile.exists() && taskData.data.arguments.firstOrNull() != "-y") {
            reporter?.publishEvent(ProcesserEncodeResultEvent(
                status = TaskStatus.Failed
            ).producedFrom(task))
            throw IllegalStateException("${cachedOutFile.absolutePath} does already exist, and arguments does not permit overwrite")
        }

        val arguments = MpegArgument()
            .inputFile(taskData.data.inputFile)
            .outputFile(cachedOutFile.absolutePath)
            .args(taskData.data.arguments)
            .withProgress(true)

        val result = getFfmpeg()
        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }
        result.run(arguments)
        if (result.result.resultCode != 0 ) {
            return ProcesserEncodeResultEvent(status = TaskStatus.Failed).producedFrom(task)
        }

        return ProcesserEncodeResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = cachedOutFile.absolutePath
            )
        ).producedFrom(task)
    }

    override fun getFfmpeg(): FFmpeg {
        return VideoFFmpeg(object : FFmpeg.Listener {
            var lastProgress: FfmpegDecodedProgress? = null
            override fun onStarted(inputFile: String) {
            }

            override fun onCompleted(inputFile: String, outputFile: String) {
                currentTask?.let {
                    coordinatorWebClient.reportProgress(
                        referenceId = it.referenceId.toString(),
                        taskId = it.taskId.toString(),
                        percent = FfmpegDecodedProgress(100, "", lastProgress?.duration ?: "", "0", estimatedCompletion = "", estimatedCompletionSeconds = 0),
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
                    coordinatorWebClient.reportProgress(
                        referenceId = it.referenceId.toString(),
                        taskId = it.taskId.toString(),
                        percent = progress,
                        ""
                    )
                }

            }
        })
    }


    class VideoFFmpeg(override val listener: Listener? = null): FFmpeg(executable = ProcesserEnv.ffmpeg, logDir = ProcesserEnv.encodeLogDirectory) {

        override fun onCreate() {
            super.onCreate()
            if (!ProcesserEnv.encodeLogDirectory.exists()) {
                ProcesserEnv.encodeLogDirectory.mkdirs()
            }
        }
    }
}