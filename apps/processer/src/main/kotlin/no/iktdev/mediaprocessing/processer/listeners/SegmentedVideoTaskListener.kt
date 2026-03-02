package no.iktdev.mediaprocessing.processer.listeners

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.processer.CoordinatorClient
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.processer.runners.ProbeRunner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.runners.segment.SegmentEncodeRunner
import no.iktdev.mediaprocessing.processer.runners.segment.SegmentConcatRunner
import no.iktdev.mediaprocessing.processer.segment.*
import no.iktdev.mediaprocessing.processer.strategy.VideoStrategy
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.EncodeProgress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import org.jetbrains.annotations.VisibleForTesting
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID

@Service
class SegmentedVideoTaskListener(
    private var coordinatorWebClient: CoordinatorClient,
    private val localProgress: LocalProgressCache,
    private val executableConfig: ExecutablesConfig,
    private val fileUtil: FileUtil,
    private val processerProperties: ProcesserProperties
) : VideoTaskListener(TaskType.CPU_INTENSIVE, processerProperties) {
    private val log = KotlinLogging.logger {}

    override val listenerStrategy: VideoStrategy = VideoStrategy.Segmented

    override fun getWorkerId() =
        "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun accept(task: Task, reporter: TaskReporter): Boolean {
        val accepts = super.accept(task, reporter)
        if (accepts) {
            log.info { "${getWorkerId()} accepts video task ${task.taskId}" }
        }
        return accepts
    }

    override suspend fun onTask(task: Task): Event? {
        val taskData = task as EncodeTask

        val ctx = createContext(taskData)

        // 1) Probe
        val totalDuration = probeDuration(ctx.input)

        // 2) Plan segments
        val segments = planSegments(input =  ctx.input, totalDuration = totalDuration, logDirectory =  ctx.logDirectory, intermediateStore = ctx.intermediateStore)

        // 3) Load checkpoints
        val checkpointStore = SegmentCheckpointStore(ctx.checkpointFile)
        val checkpoint = checkpointStore.load()

        // 4) Process segments
        processAllSegments(
            segments = segments,
            checkpointStore = checkpointStore,
            checkpoint = checkpoint,
            ctx = ctx
        )

        // 5) Concat
        concatSegments(segments, ctx)

        // 6) Collect logs
        val mergedLog = collectLogs(ctx.logDirectory, ctx.taskStartTime)

        // 7) Return result
        return ProcesserEncodeResultEvent(
            status = TaskStatus.Completed,
            logFile = mergedLog.absolutePath,
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = ctx.output.absolutePath
            )
        ).producedFrom(task)
    }

    // -------------------------------
    //  Context creation
    // -------------------------------

    private fun createContext(taskData: EncodeTask): SegmentedRunnerContext {
        val input = File(taskData.data.inputFile)

        val intermediateStore = fileUtil.getTemporaryStoreFolder(taskData.data.outputFolderName)
            .apply { if (!this.exists()) mkdirs() }

        val output = intermediateStore.using(taskData.data.outputFileName)
            .apply { if (!this.parentFile.exists()) parentFile.mkdirs() }

        val logDirectory = fileUtil.getLogDirectory()
            .using("encode_segment", taskData.taskId.toString())

        val baseOutputFileName = File(taskData.data.outputFileName).nameWithoutExtension
        val checkpointFile = intermediateStore
            .using("$baseOutputFileName - CHECKPOINTS.json")

        return SegmentedRunnerContext(
            task = taskData,
            input = input,
            output = output,
            intermediateStore = intermediateStore,
            logDirectory = logDirectory,
            checkpointFile = checkpointFile,
            taskStartTime = System.currentTimeMillis(),
            args = taskData.data.arguments
        )
    }

    // -------------------------------
    //  Helpers
    // -------------------------------

    private suspend fun probeDuration(input: File): Double {
        val result = ProbeRunner(input, executableConfig.ffprobe).run()
        return when (result) {
            is RunnerResult.Success -> result.payload.requireTotalDuration()
            is RunnerResult.Reject -> throw IllegalStateException("Probe failed: ${result.reason}")
        }
    }

    private fun planSegments(input: File, totalDuration: Double, logDirectory: File, intermediateStore: File): List<Segment> {
        val planner = SegmentPlanner(segmentLength = 60.0)
        val subfolder = intermediateStore.using("segments").also {
            if (it.exists()) { it.mkdirs() }
        }
        return planner.plan(input, totalDuration, subfolder)
    }

    private suspend fun processAllSegments(
        segments: List<Segment>,
        checkpointStore: SegmentCheckpointStore,
        checkpoint: SegmentCheckpointStore.Checkpoint,
        ctx: SegmentedRunnerContext
    ) {
        for (segment in segments) {
            processSegment(
                segment = segment,
                checkpointStore = checkpointStore,
                checkpoint = checkpoint,
                segments = segments,
                ctx = ctx
            )
        }
    }

    @VisibleForTesting
    internal suspend fun processSegment(
        segment: Segment,
        checkpointStore: SegmentCheckpointStore,
        checkpoint: SegmentCheckpointStore.Checkpoint,
        segments: List<Segment>,
        ctx: SegmentedRunnerContext
    ) {
        val outputFile = segment.output

        if (segment.index in checkpoint.completed) {
            if (!outputFile.exists()) {
                throw IllegalStateException(
                    "Checkpoint says segment ${segment.index} is done, but file is missing: ${outputFile.absolutePath}"
                )
            }
            return
        }

        if (outputFile.exists()) {
            log.warn { "Segment ${segment.index} exists but is not checkpointed. Deleting stale file." }
            outputFile.delete()
        }

        val ffmpeg = getFfmpeg(
            listener = listener,
            execPath = executableConfig.ffmpeg,
            logDirectory = ctx.logDirectory
        )

        val runner = SegmentEncodeRunner(segment = segment, input = ctx.input, args =  ctx.args, ffmpegInstance =  ffmpeg)
        when (val result = runner.run()) {
            is RunnerResult.Success -> {
                checkpointStore.markCompleted(segment.index)
                reportGlobalProgress(ctx.task, segments, checkpointStore)
            }
            is RunnerResult.Reject -> {
                throw FfmpegFailedException(ffmpeg.logFile, result.reason)
            }
        }
    }

    private suspend fun concatSegments(
        segments: List<Segment>,
        ctx: SegmentedRunnerContext
    ) {
        val ffmpeg = getFfmpeg(
            listener = listener,
            execPath = executableConfig.ffmpeg,
            logDirectory = ctx.logDirectory
        )

        val runner = SegmentConcatRunner(segments, ctx.intermediateStore, ctx.output, ffmpeg)
        when (val result = runner.run()) {
            is RunnerResult.Success -> Unit
            is RunnerResult.Reject -> throw IllegalStateException(result.reason)
        }
    }

    @VisibleForTesting
    internal fun collectLogs(logDirectory: File, taskStartTime: Long): File {
        val merged = File(logDirectory, "merged.log")

        val logs = logDirectory.walk()
            .filter { it.isFile && it.extension == "log" }
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

    private fun ProbeRunner.ProbePayload.requireTotalDuration(): Double {
        val raw = format.duration
            ?: throw IllegalStateException("FFprobe returned no duration")
        return raw.toDoubleOrNull()
            ?: throw IllegalStateException("Invalid duration value: '$raw'")
    }

    private fun reportGlobalProgress(
        task: Task,
        segments: List<Segment>,
        checkpointStore: SegmentCheckpointStore
    ) {
        val cp = checkpointStore.load()

        val total = segments.sumOf { it.duration }
        val done = segments.filter { it.index in cp.completed }.sumOf { it.duration }

        val percent = ((done / total) * 100).toInt()

        val progress = EncodeProgress(
            progress = percent,
            ffmpegDecodedProgress = FfmpegDecodedProgress(
                progress = percent,
                time = "",
                duration = total.toString(),
                speed = "",
                estimatedCompletion = "",
                estimatedCompletionSeconds = 0
            ),
         ""
        )
        reporter?.updateProgress(task.referenceId, task.taskId, progress)
    }

    val listener = object : FFmpeg.Listener {
        override fun onStarted(inputFile: String) {}
        override fun onCompleted(inputFile: String, outputFile: String) {}
        override fun onProgressChanged(inputFile: String, progress: FfmpegDecodedProgress) {}
    }
}
