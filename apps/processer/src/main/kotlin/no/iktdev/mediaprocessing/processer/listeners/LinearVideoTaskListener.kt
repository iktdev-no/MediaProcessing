package no.iktdev.mediaprocessing.processer.listeners

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.eventi.tasks.TaskValidator
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.processer.CoordinatorClient
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.processer.linear.LinearContextFactory
import no.iktdev.mediaprocessing.processer.linear.LinearProcessor
import no.iktdev.mediaprocessing.processer.progress.DynamicProgressWeights
import no.iktdev.mediaprocessing.processer.progress.LinearProgressListener
import no.iktdev.mediaprocessing.processer.services.ProcessService
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.LinearEncodeTask
import org.jetbrains.annotations.VisibleForTesting
import org.springframework.stereotype.Service
import java.util.*

@Service
class LinearVideoTaskListener(
    private var coordinatorWebClient: CoordinatorClient,
    private val localProgress: LocalProgressCache,
    private val executableConfig: ExecutablesConfig,
    private val fileUtil: FileUtil,
    private val processerProperties: ProcesserProperties,
    private val processService: ProcessService? = null
) : VideoTaskListener(TaskType.CPU_INTENSIVE, executableConfig) {
    private val log = KotlinLogging.logger {}

    init {
        setUseSharedBusyState(true)
    }

    override fun getWorkerId() =
        "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun supports(task: Task): Boolean =
        task is LinearEncodeTask

    override fun accept(task: Task, reporter: TaskReporter, validator: TaskValidator?): Boolean {
        val accepts = super.accept(task, reporter, validator)
        if (accepts) {
            log.info { "${getWorkerId()} accepts video task ${task.taskId}" }
        }
        return accepts
    }

    override suspend fun onTask(task: Task): Event? {
        val taskData = task as LinearEncodeTask

        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }

        val weights = DynamicProgressWeights(video = task.data.videoInstruction, audio = task.data.audioInstructions)
            .compute()

        val ctx = LinearContextFactory(fileUtil).createContext(taskData)

        val progressListener = LinearProgressListener(task, reporter, weights) { taskId, progress ->
            localProgress.update(taskId, progress)
        }

        if (ctx.output.exists() && taskData.data.videoInstruction.output?.overwrite != true) {
            reporter?.publishEvent(
                ProcesserEncodeResultEvent(
                    status = TaskStatus.Failed,
                    error = "${ctx.output.absolutePath} does already exist, and arguments does not permit overwrite"
                ).producedFrom(task)
            )
            return null
        }

        val processor = LinearProcessor(this, progressListener, processService)

        val videoTrack = processor.processVideo(ctx)

        val audioTracks = processor.processAudio(ctx)

        val finalFile = processor.processMerge(ctx, audioTracks, videoTrack.output)


        val mergedLog = collectLogs(ctx.logDirectory, ctx.taskStartTime)


        return ProcesserEncodeResultEvent(
            status = TaskStatus.Completed,
            logFile = mergedLog.absolutePath,
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = finalFile.absolutePath
            )
        ).producedFrom(task)
    }

    @VisibleForTesting
    internal fun collectLogs(logDirectory: IFile, taskStartTime: Long): IFile {
        val merged = logDirectory.using("merged.log")

        val logs = logDirectory.walk()
            .filter { it.isFile() && it.extension() == "log" }
            .filter { it.lastModified() >= taskStartTime }
            .sortedBy { it.lastModified() }
            .toList()

        merged.printWriter().use { writer ->
            logs.forEach { file ->
                writer.println("===== LOG FROM ${file.name} =====")
                writer.println(file.readText())
                writer.println()
            }
        }

        return merged
    }


}