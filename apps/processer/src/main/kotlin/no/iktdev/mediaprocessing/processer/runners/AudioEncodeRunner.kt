package no.iktdev.mediaprocessing.processer.runners

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.AudioStreamConfig
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.ConcatInputConfig
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputConfig

class AudioEncodeRunner(
    private val audioInstruction: FFmpegInstructions,
    private val outputDirectory: IFile,
    private val ffmpegInstance: FFmpeg
): Runner() {
    override suspend fun run(): RunnerResult<AudioEncodePayload> {

        val dsl = ffmpeg {
            fromInstructions(audioInstruction)
            outputDirectory(outputDirectory)
        }

        ffmpegInstance.run(dsl)
        val result = ffmpegInstance.result
        val outFile = resolveExpectedFullPath()
        if (!outFile.exists()) {
            RunnerResult.Reject("Attempted to verify placement of ${dsl.outputFile()} using ${outFile.absolutePath}, but it was not in expected folder ${outputDirectory.absolutePath}")
        }

        return if (result.resultCode == 0) {
            RunnerResult.Success(AudioEncodePayload(outFile, getAudioMetadata()))
        } else {
            RunnerResult.Reject("Audio encode failed with code ${result.resultCode}")
        }
    }
    data class AudioEncodePayload(
        val output: IFile,
        val meta: AudioStreamConfig
    )

    fun resolveExpectedFullPath(): IFile {
        val useFileName = audioInstruction.output?.path ?: throw IllegalStateException("Output is missing on audio instruction")
        return outputDirectory.using(useFileName)
    }

    fun getAudioMetadata(): AudioStreamConfig {
        val allInputs = audioInstruction.inputs.files()

        // 1) Concat mode? → Ikke lov
        require(audioInstruction.inputs.concatInput == null) { "Concat not allowed in AudioEncodeRunner" }

        // 2) Normal mode → hent InputConfig
        val inputConfigs = allInputs.filterIsInstance<InputConfig>()
        if (inputConfigs.size != 1) {
            throw IllegalStateException("Audio instruction expects exactly 1 input file, but found ${inputConfigs.size}")
        }

        // 3) Hent audio streams
        val audioStreams = inputConfigs
            .flatMap { it.audioStreams }

        if (audioStreams.size != 1) {
            throw IllegalStateException("Audio instruction expects exactly 1 audio stream, but found ${audioStreams.size}")
        }

        return audioStreams.first()
    }

}