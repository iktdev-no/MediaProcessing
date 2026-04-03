package no.iktdev.mediaprocessing.processer.processors.segment

import mu.KotlinLogging
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.util.getAudioMetadata
import no.iktdev.mediaprocessing.ffmpeg.util.resolveExpectedFullPath
import no.iktdev.mediaprocessing.processer.context.CheckpointStore
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.context.SegmentedRunnerContext
import no.iktdev.mediaprocessing.processer.processors.AudioProcessor
import no.iktdev.mediaprocessing.processer.progress.SegmentedProgressListener
import no.iktdev.mediaprocessing.processer.runners.AudioEncodeRunner
import no.iktdev.mediaprocessing.processer.runners.AudioVideoMergeRunner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.services.ProcessService
import java.util.UUID

class SegmentedAudioProcessor(
    private val ffProvider: FfProvider,
    private val progressListener: SegmentedProgressListener,
    private val processService: ProcessService? = null
): AudioProcessor(ffProvider) {
    private val log = KotlinLogging.logger {}

    suspend fun encodeAudioStreams(
        ctx: SegmentedRunnerContext
    ): List<AudioEncodeRunner.AudioEncodePayload> {

        val outputs = mutableListOf<AudioEncodeRunner.AudioEncodePayload>()

        val outStore = ctx.intermediateStore.using("audio").apply { mkdirs() }

        val checkpointStore = CheckpointStore(ctx.audioCheckpointFile)
        val checkpoint = checkpointStore.load()
        val done = checkpoint.completed
        val totalTracks = ctx.audioInstructions.size

        val toEncode = ctx.audioInstructions.mapIndexed { index, instruct ->
            if (index !in done) {
                return@mapIndexed AudioEncodeItem(index, instruct, outStore)
            }

            val outFile = instruct.resolveExpectedFullPath(outStore)
            if (!outFile.exists()) {
                val workFile = instruct.output?.workFile?.let { outStore.using(it) }
                if (workFile != null && workFile.exists()) {
                    log.warn { "Final audio file missing but work file exists. Re-running encoding for track $index." }
                    workFile.delete()
                    done.remove(index) // må re-encode
                } else {
                    throw IllegalStateException(
                        "Audio checkpoint says track $index is done, but file missing: ${outFile.absolutePath}"
                    )
                }
            } else {
                outputs += AudioEncodeRunner.AudioEncodePayload(
                    outFile,
                    null,
                    instruct.getAudioMetadata()
                )
                val doneTracks = checkpoint.completed.size
                progressListener.onAudioProgress(doneTracks, totalTracks)
            }
            null
        }.filterNotNull()

        super.encodeAudioStreams(ctx.task.taskId,toEncode, logDirectory = ctx.logDirectory, null) { index, payload ->
            checkpointStore.markCompleted(index)
            outputs += payload

            val doneTracks = checkpointStore.load().completed.size
            progressListener.onAudioProgress(doneTracks, totalTracks)
        }

        return outputs;
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
            taskId = ctx.task.taskId,
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