package no.iktdev.mediaprocessing.processer.linear

import mu.KotlinLogging
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.context.LinearRunnerContext
import no.iktdev.mediaprocessing.processer.processors.linear.LinearAudioProcessor
import no.iktdev.mediaprocessing.processer.processors.linear.LinearVideoProcessor
import no.iktdev.mediaprocessing.processer.progress.LinearProgressListener
import no.iktdev.mediaprocessing.processer.runners.AudioEncodeRunner
import no.iktdev.mediaprocessing.processer.runners.AudioVideoMergeRunner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.runners.VideoEncodeRunner

class LinearProcessor(
    private val ffProvider: FfProvider,
    private val progressListener: LinearProgressListener
) {
    private val log = KotlinLogging.logger {}


    suspend fun processVideo(
        ctx: LinearRunnerContext
    ): VideoEncodeRunner.VideoEncodeResult {
        return LinearVideoProcessor(ffProvider, progressListener).encodeVideo(ctx)
    }

    suspend fun processAudio(
        ctx: LinearRunnerContext
    ): List<AudioEncodeRunner.AudioEncodePayload> {
        return LinearAudioProcessor(
            ffProvider, progressListener
        ).encodeAudio(ctx)
    }

    suspend fun processMerge(
        ctx: LinearRunnerContext,
        audioFiles: List<AudioEncodeRunner.AudioEncodePayload>,
        videoFile: IFile
    ): IFile {
        progressListener.onMergeProgress(0)

        val ffmpeg = ffProvider.getFfmpeg(
            logDirectory = ctx.logDirectory
        )

        val mergeRunner = AudioVideoMergeRunner(
            taskId = ctx.task.taskId,
            videoFile = videoFile,
            audioFiles = audioFiles,
            output = ctx.output,
            ffmpegInstance = ffmpeg
        )

        if (ctx.output.exists()) {
            progressListener.onMergeProgress(100)
        }

        return when (val result = mergeRunner.run()) {
            is RunnerResult.Success -> {
                progressListener.onMergeProgress(100)
                result.payload.output
            }
            is RunnerResult.Reject -> throw IllegalStateException(result.reason)
        }
    }

}