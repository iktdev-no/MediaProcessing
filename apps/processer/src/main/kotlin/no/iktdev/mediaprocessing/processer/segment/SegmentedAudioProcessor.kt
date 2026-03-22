package no.iktdev.mediaprocessing.processer.segment

import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.processer.context.CheckpointStore
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.listeners.FfTaskListener.FfmpegFailedException
import no.iktdev.mediaprocessing.processer.runners.AudioEncodeRunner
import no.iktdev.mediaprocessing.processer.runners.AudioVideoMergeRunner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.files.IFile

class SegmentedAudioProcessor(
    private val ffProvider: FfProvider,
    private val progressListener: SegmentedProgressListener
) {

    suspend fun encodeAudioStreams(
        ctx: SegmentedRunnerContext
    ): List<AudioEncodeRunner.AudioEncodePayload> {

        val outputs = mutableListOf<AudioEncodeRunner.AudioEncodePayload>()

        val checkpointStore = CheckpointStore(ctx.audioCheckpointFile)
        val checkpoint = checkpointStore.load()
        val totalTracks = ctx.audioInstructions.size

        ctx.audioInstructions.forEachIndexed { index, instruct ->

            val outStore = ctx.intermediateStore
                .using("audio")
                .apply { mkdirs() }

            val ffmpeg = ffProvider.getFfmpeg(
                logDirectory = ctx.logDirectory
            )

            val runner = AudioEncodeRunner(
                audioInstruction = instruct,
                outputDirectory = outStore,
                ffmpegInstance = ffmpeg
            )

            // Allerede ferdig?
            if (index in checkpoint.completed) {
                val useFile = runner.resolveExpectedFullPath()
                if (!useFile.exists()) {
                    throw IllegalStateException(
                        "Audio checkpoint says track $index is done, but file missing: ${useFile.absolutePath}"
                    )
                }
                outputs.add(AudioEncodeRunner.AudioEncodePayload(useFile, runner.getAudioMetadata()))
                val doneTracks = checkpoint.completed.size
                progressListener.onAudioProgress(doneTracks, totalTracks)
                return@forEachIndexed
            }

            when (val result = runner.run()) {
                is RunnerResult.Success -> {
                    checkpointStore.markCompleted(index)
                    outputs += result.payload
                    val doneTracks = checkpointStore.load().completed.size
                    progressListener.onAudioProgress(doneTracks, totalTracks)
                }
                is RunnerResult.Reject -> {
                    throw FfmpegFailedException(ffmpeg.logFile, result.reason)
                }
            }
        }

        return outputs
    }

    suspend fun mergeAudioIntoVideo(
        ctx: SegmentedRunnerContext,
        audioFiles: List<AudioEncodeRunner.AudioEncodePayload>
    ): IFile {

        progressListener.onMergeProgress(0.0)

        val finalOutput = ctx.intermediateStore
            .using(ctx.task.data.outputFileName)
            .apply { parentFile.mkdirs() }

        val ffmpeg = ffProvider.getFfmpeg(
            logDirectory = ctx.logDirectory
        )

        val runner = AudioVideoMergeRunner(
            videoFile = ctx.output,
            audioFiles = audioFiles,
            output = finalOutput,
            ffmpegInstance = ffmpeg
        )

        return when (val result = runner.run()) {
            is RunnerResult.Success -> {
                progressListener.onMergeProgress(1.0)
                finalOutput
            }
            is RunnerResult.Reject -> throw IllegalStateException(result.reason)
        }
    }
}