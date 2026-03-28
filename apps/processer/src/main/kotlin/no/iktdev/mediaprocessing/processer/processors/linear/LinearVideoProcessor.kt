package no.iktdev.mediaprocessing.processer.processors.linear

import mu.KotlinLogging
import no.iktdev.eventi.models.Task
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.context.LinearRunnerContext
import no.iktdev.mediaprocessing.processer.listeners.FfTaskListener.FfmpegFailedException
import no.iktdev.mediaprocessing.processer.progress.LinearProgressListener
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.runners.VideoEncodeRunner

class LinearVideoProcessor(
    private val ffProvider: FfProvider,
    private val progressListener: LinearProgressListener
) {

    private val log = KotlinLogging.logger {}

    suspend fun encodeVideo(ctx: LinearRunnerContext): VideoEncodeRunner.VideoEncodeResult {
        val listener = createListener(ctx.task)
        val noAudioMidfix = ctx.output.let { file ->
            val ext =  "noaudio." + file.extension()
            file.parentFile.using("${file.nameWithoutExtension}.$ext")
        }
        if (noAudioMidfix.exists()) {
            log.info("Found existing video file ${noAudioMidfix.absolutePath}, returning this instead")
            return VideoEncodeRunner.VideoEncodeResult(noAudioMidfix)
        }

        val cachedOut = noAudioMidfix.parentFile.using(
            "${noAudioMidfix.nameWithoutExtension}.work.${noAudioMidfix.extension()}"
        )

        if (cachedOut.exists()) {
            log.info("Found existing work file ${cachedOut.absolutePath}, as this is incomplete and we are restarting, this will be deleted")
            val deleted = cachedOut.delete()
            log.warn { "Work file ${cachedOut.absolutePath} was ${if (deleted) "deleted" else "NOT deleted"}" }
        }


        val ffmpeg = ffProvider.getFfmpeg(logDirectory = ctx.logDirectory, listener = listener)

        val runner = VideoEncodeRunner(
            ctx.videoInstruction,
            ctx.intermediateStore,
            outputFile = noAudioMidfix,
            ffmpeg
        )

        when (val result = runner.run()) {
            is RunnerResult.Success -> {
                return result.payload
            }
            is RunnerResult.Reject -> {
                throw FfmpegFailedException(ffmpeg.logFile, result.reason)
            }
        }

    }

    private fun createListener(task: Task) = object : FFmpeg.Listener {
        var lastProgress: FfmpegDecodedProgress? = null
        override fun onStarted(inputFile: String) {
        }

        override fun onCompleted(inputFile: String, outputFile: String) {
            task.let {
                val progress = FfmpegDecodedProgress(
                    100,
                    "",
                    lastProgress?.duration ?: "",
                    "0",
                    estimatedCompletion = "",
                    estimatedCompletionSeconds = 0
                )
                progressListener.onVideoProgress(progress)
            }
        }

        override fun onProgressChanged(
            inputFile: String,
            progress: FfmpegDecodedProgress
        ) {
            lastProgress = progress
            progressListener.onVideoProgress(progress)


        }
    }


}