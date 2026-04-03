package no.iktdev.mediaprocessing.processer.processors.linear

import mu.KotlinLogging
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.ffmpeg.util.getAudioMetadata
import no.iktdev.mediaprocessing.ffmpeg.util.resolveExpectedFullPath
import no.iktdev.mediaprocessing.processer.context.CheckpointStore
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.context.LinearRunnerContext
import no.iktdev.mediaprocessing.processer.processors.AudioProcessor
import no.iktdev.mediaprocessing.processer.progress.LinearProgressListener
import no.iktdev.mediaprocessing.processer.runners.AudioEncodeRunner
import no.iktdev.mediaprocessing.processer.services.ProcessService

class LinearAudioProcessor(
    private val ffProvider: FfProvider,
    private val progressListener: LinearProgressListener,
    private val processService: ProcessService? = null
): AudioProcessor(ffProvider) {

    private val log = KotlinLogging.logger {}


    suspend fun encodeAudio(ctx: LinearRunnerContext): List<AudioEncodeRunner.AudioEncodePayload> {
        val outputs = mutableListOf<AudioEncodeRunner.AudioEncodePayload>()

        val outStore = ctx.intermediateStore.using("audio").apply { mkdirs() }

        val checkpointStore = CheckpointStore(ctx.audioCheckpointFile)
        val checkpoint = checkpointStore.load()
        val done = checkpoint.completed.toMutableSet()
        val total = ctx.audioInstructions.size



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
                progressListener.onAudioProgress(index, FfmpegDecodedProgress(
                    progress = 100,
                    time = "",
                    duration = "",
                    speed = "",

                ), total)
            }
            null
        }.filterNotNull()

        super.encodeAudioStreams(
            taskId = ctx.task.taskId,
            toEncode,
            logDirectory = ctx.logDirectory,
            onProgress = { index, decoded ->
                progressListener.onAudioProgress(index, decoded, total)
            }) { index, payload ->
            checkpointStore.markCompleted(index)
            outputs += payload
            progressListener.onAudioProgress(index, FfmpegDecodedProgress(100, "", "", ""), total)
        }

        return outputs;
    }

}