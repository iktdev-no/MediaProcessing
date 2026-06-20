package no.iktdev.mediaprocessing.processer.listeners

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.eventi.tasks.TaskValidator
import no.iktdev.mediaprocessing.processer.CoordinatorClient
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.processer.context.CheckpointStore
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.processer.processors.segment.SegmentedAudioProcessor
import no.iktdev.mediaprocessing.processer.processors.segment.SegmentedContextFactory
import no.iktdev.mediaprocessing.processer.processors.segment.SegmentedVideoProcessor
import no.iktdev.mediaprocessing.processer.progress.SegmentedProgressListener
import no.iktdev.mediaprocessing.processer.services.ProcessService
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.SegmentedEncodeTask
import org.jetbrains.annotations.VisibleForTesting
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SegmentedVideoTaskListener(
    private var coordinatorWebClient: CoordinatorClient,
    private val localProgress: LocalProgressCache,
    private val executableConfig: ExecutablesConfig,
    private val fileUtil: FileUtil,
    private val processService: ProcessService? = null
) : VideoTaskListener(TaskType.CPU_INTENSIVE, executableConfig) {
    private val log = KotlinLogging.logger {}

    init {
        setUseSharedBusyState(true)
    }

    override fun getWorkerId() =
        "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun supports(task: Task): Boolean =
        task is SegmentedEncodeTask

    override fun accept(task: Task, reporter: TaskReporter, validator: TaskValidator?): Boolean {
        val accepts = super.accept(task, reporter, validator)
        if (accepts) {
            log.info { "${getWorkerId()} accepts video task ${task.taskId}" }
        }
        return accepts
    }

    override suspend fun onTask(task: Task): Event? {
        val taskData = task as SegmentedEncodeTask

        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }

        val ctx = SegmentedContextFactory(fileUtil).createContext(taskData)

        val progressListener = SegmentedProgressListener(task, reporter) { taskId, progress ->
            localProgress.update(taskId, progress)
        }

        val videoProcessor = SegmentedVideoProcessor(this, progressListener, processService)

        // 1) Probe
        val totalDuration = videoProcessor.probeDuration(ctx.input)

        // 2) Plan segments
        val segments = videoProcessor.planSegments(input =  ctx.input, totalDuration = totalDuration, logDirectory =  ctx.logDirectory, intermediateStore = ctx.intermediateStore)

        // 3) Load checkpoints
        val checkpointStore = CheckpointStore(ctx.videoCheckpointFile)
        val checkpoint = checkpointStore.load()

        // 4) Process segments
        videoProcessor.processAllSegments(
            segments = segments,
            checkpointStore = checkpointStore,
            checkpoint = checkpoint,
            ctx = ctx
        )

        // 5) Concat
        val concatted = videoProcessor.concatSegments(segments, ctx)

        val audioProcessor = SegmentedAudioProcessor(this, progressListener)
        // 6) Encode audio (restart-sikkert)
        val audioTrackFiles = audioProcessor.encodeAudioStreams(ctx)

        // 7) Merge video + audio
        val finalOutput = audioProcessor.mergeAudioIntoVideo(ctx, audioTrackFiles, concatted)

        if (finalOutput.absolutePath != ctx.output.absolutePath) {
            error("Mismatch between actual out and expected out in context: ${finalOutput.absolutePath} != ${ctx.output.absolutePath}")
        }

        // 8) Collect logs
        val mergedLog = collectLogs(ctx.logDirectory, ctx.taskStartTime)

        // 9) Return result
        return ProcesserEncodeResultEvent(
            status = TaskStatus.Completed,
            logFile = mergedLog.absolutePath,
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = ctx.output.absolutePath,
                cachedSegmentFiles = segments.map { it.output.absolutePath }
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
