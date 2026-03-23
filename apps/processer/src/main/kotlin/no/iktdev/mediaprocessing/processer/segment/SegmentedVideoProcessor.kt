package no.iktdev.mediaprocessing.processer.segment

import mu.KotlinLogging
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.processer.context.CheckpointStore
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.listeners.FfTaskListener.FfmpegFailedException
import no.iktdev.mediaprocessing.processer.runners.ProbeRunner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.runners.segment.SegmentConcatRunner
import no.iktdev.mediaprocessing.processer.runners.segment.SegmentEncodeRunner
import no.iktdev.files.IFile
import org.jetbrains.annotations.VisibleForTesting

class SegmentedVideoProcessor(
    private val ffProvider: FfProvider,
    private val progressListener: SegmentedProgressListener
) {
    private val log = KotlinLogging.logger {}


    suspend fun probeDuration(input: IFile): Double {
        val result = ProbeRunner(input, ffProvider.getExecutableFfprobe()).run()
        return when (result) {
            is RunnerResult.Success -> result.payload.requireTotalDuration()
            is RunnerResult.Reject -> throw IllegalStateException("Probe failed: ${result.reason}")
        }
    }

    private fun ProbeRunner.ProbePayload.requireTotalDuration(): Double {
        val raw = format.duration
            ?: throw IllegalStateException("FFprobe returned no duration")
        return raw.toDoubleOrNull()
            ?: throw IllegalStateException("Invalid duration value: '$raw'")
    }

    fun planSegments(input: IFile, totalDuration: Double, logDirectory: IFile, intermediateStore: IFile): List<Segment> {
        val planner = SegmentPlanner(segmentLength = 60.0)
        val subfolder = intermediateStore.using("video_segments").also {
            if (!it.exists()) { it.mkdirs() }
        }
        return planner.plan(totalDuration, subfolder)
    }

    suspend fun processAllSegments(
        segments: List<Segment>,
        checkpointStore: CheckpointStore,
        checkpoint: CheckpointStore.Checkpoint,
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
        checkpointStore: CheckpointStore,
        checkpoint: CheckpointStore.Checkpoint,
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

        val ffmpeg = ffProvider.getFfmpeg(
            logDirectory = ctx.logDirectory
        )

        val runner = SegmentEncodeRunner(
            segment = segment,
            videoInstructions = ctx.videoInstruction,
            ffmpegInstance = ffmpeg
        )

        when (val result = runner.run()) {
            is RunnerResult.Success -> {
                checkpointStore.markCompleted(segment.index)
                // 🔥 NEW: calculate weighted video progress
                val cp = checkpointStore.load()
                val totalSeconds = segments.sumOf { it.duration }
                val doneSeconds = segments
                    .filter { it.index in cp.completed }
                    .sumOf { it.duration }

                progressListener.onVideoProgress(doneSeconds, totalSeconds)
            }
            is RunnerResult.Reject -> {
                throw FfmpegFailedException(ffmpeg.logFile, result.reason)
            }
        }
    }

    suspend fun concatSegments(
        segments: List<Segment>,
        ctx: SegmentedRunnerContext
    ): IFile {
        progressListener.onConcatProgress(0.0)


        val ffmpeg = ffProvider.getFfmpeg(
            logDirectory = ctx.logDirectory
        )

        val noAudioMidfix = ctx.output.let { file ->
            val ext =  "noaudio." + file.extension()
            file.parentFile.using("${file.nameWithoutExtension}.$ext")
        }

        if (noAudioMidfix.exists()) {
            return noAudioMidfix
        }

        val runner = SegmentConcatRunner(segments, ctx.intermediateStore, noAudioMidfix, ffmpeg)
        return when (val result = runner.run()) {
            is RunnerResult.Success -> {
                progressListener.onConcatProgress(1.0)
                result.payload.output
            }
            is RunnerResult.Reject -> throw IllegalStateException(result.reason)
        }
    }
}