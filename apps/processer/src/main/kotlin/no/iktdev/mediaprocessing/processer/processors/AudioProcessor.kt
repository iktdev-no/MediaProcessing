package no.iktdev.mediaprocessing.processer.processors

import mu.KotlinLogging
import no.iktdev.eventi.models.Task
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.ffmpeg.util.resolveExpectedFullPath
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.listeners.FfTaskListener.FfmpegFailedException
import no.iktdev.mediaprocessing.processer.runners.AudioEncodeRunner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import java.util.UUID

open class AudioProcessor(
    private val ffProvider: FfProvider,
) {
    private val log = KotlinLogging.logger {}

    data class AudioEncodeItem(
        val index: Int,
        val instruction: FFmpegInstructions,
        val outStore: IFile,
    )

    protected suspend fun encodeAudioStreams(
        taskId: UUID,
        streams: List<AudioEncodeItem>,
        logDirectory: IFile,
        onProgress: ((index: Int, progress: FfmpegDecodedProgress) -> Unit)? = null,
        onInstructionCompleted: (index: Int, payload: AudioEncodeRunner.AudioEncodePayload) -> Unit
    ): List<AudioEncodeRunner.AudioEncodePayload> {

        val outputs = mutableListOf<AudioEncodeRunner.AudioEncodePayload>()

        streams.forEach { item ->

            val listener = object : FFmpeg.Listener {
                private var lastUpdate: FfmpegDecodedProgress? = null
                override fun onStarted(inputFile: String) {
                }

                override fun onCompleted(inputFile: String, outputFile: String) {
                    onProgress?.invoke(item.index, lastUpdate?.copy(
                        time = "",
                    ) ?: FfmpegDecodedProgress(progress = 100, "", "", ""))
                }

                override fun onProgressChanged(inputFile: String, progress: FfmpegDecodedProgress) {
                    lastUpdate = progress
                    onProgress?.invoke(item.index, progress)
                }
            }

            val ffmpeg = ffProvider.getFfmpeg(logDirectory = logDirectory, listener = listener)
            val outputFile = item.instruction.resolveExpectedFullPath(item.outStore)


            val runner = AudioEncodeRunner(
                taskId = taskId,
                audioInstruction = item.instruction,
                outputDirectory = item.outStore,
                ffmpegInstance = ffmpeg,
                outputFile = outputFile
            )


            if (outputFile.exists()) {
                log.warn { "Stale audio file found for track ${item.index}. Deleting: ${outputFile.absolutePath}" }
                outputFile.delete()
            }

            when (val result = runner.run()) {
                is RunnerResult.Success -> {
                    outputs += result.payload
                    onInstructionCompleted(item.index, result.payload)
                }

                is RunnerResult.Reject -> {
                    throw FfmpegFailedException(ffmpeg.logFile, result.reason)
                }
            }
        }

        return outputs
    }

}

