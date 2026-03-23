package no.iktdev.mediaprocessing.processer.segment

import mu.KotlinLogging
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
    private val log = KotlinLogging.logger {}

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

            val outputFile = runner.resolveExpectedFullPath()

            // --- CASE 1: Checkpoint says done ---
            if (index in checkpoint.completed) {
                if (!outputFile.exists()) {
                    val workFile = instruct.output?.workFile?.let { outStore.using(it) }
                    if (workFile != null && workFile.exists()) {
                        log.warn { "Final audio file missing but work file exists. Re-running encoding for track $index." }
                        workFile.delete()
                    } else {
                        throw IllegalStateException(
                            "Audio checkpoint says track $index is done, but file missing: ${outputFile.absolutePath}"
                        )
                    }
                } else {
                    outputs += AudioEncodeRunner.AudioEncodePayload(
                        outputFile,
                        runner.getAudioMetadata()
                    )

                    val doneTracks = checkpoint.completed.size
                    progressListener.onAudioProgress(doneTracks, totalTracks)
                    return@forEachIndexed
                }
            }

            // --- CASE 2: File exists but checkpoint does NOT say done → stale file ---
            if (outputFile.exists()) {
                log.warn { "Audio track $index exists but is not checkpointed. Deleting stale file: ${outputFile.absolutePath}" }
                outputFile.delete()
            }

            // --- CASE 3: Run encoding ---
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
        audioFiles: List<AudioEncodeRunner.AudioEncodePayload>,
        concatted: IFile
    ): IFile {

        progressListener.onMergeProgress(0.0)

        val finalOutput = ctx.intermediateStore
            .using(ctx.task.data.outputFileName)
            .apply { parentFile.mkdirs() }

        val ffmpeg = ffProvider.getFfmpeg(
            logDirectory = ctx.logDirectory
        )

        val runner = AudioVideoMergeRunner(
            videoFile = concatted,
            audioFiles = audioFiles,
            output = finalOutput,
            ffmpegInstance = ffmpeg
        )

        if (finalOutput.exists()) {
            progressListener.onMergeProgress(1.0)
            return finalOutput
        }

        return when (val result = runner.run()) {
            is RunnerResult.Success -> {
                progressListener.onMergeProgress(1.0)
                finalOutput
            }
            is RunnerResult.Reject -> throw IllegalStateException(result.reason)
        }
    }
}