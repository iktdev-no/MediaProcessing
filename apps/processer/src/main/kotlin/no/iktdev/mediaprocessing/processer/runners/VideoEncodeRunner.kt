package no.iktdev.mediaprocessing.processer.runners

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg

class VideoEncodeRunner(
    private val videoInstructions: FFmpegInstructions,
    private val outputDirectory: IFile,
    private val outputFile: IFile,
    private val ffmpegInstance: FFmpeg
): Runner() {
    override suspend fun run(): RunnerResult<VideoEncodeResult> {
        val dsl = ffmpeg {
            fromInstructions(videoInstructions)
            outputDirectory(outputDirectory)
        }

        ffmpegInstance.run(dsl)
        val result = ffmpegInstance.result
        if (!outputFile.exists()) {
            return RunnerResult.Reject("Attempted to verify placement of ${dsl.outputFile()} using ${outputFile.absolutePath}, but it was not in expected folder ${outputDirectory.absolutePath}")
        }

        return if (result.resultCode == 0) {
            RunnerResult.Success(VideoEncodeResult(
                output = outputFile,
                logFile = ffmpegInstance.logFile,
            ))
        } else {
            RunnerResult.Reject("Video encode failed with code ${result.resultCode}")
        }
    }

    data class VideoEncodeResult(
        val output: IFile,
        val logFile: IFile? = null
    )

}