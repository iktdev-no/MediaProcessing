package no.iktdev.mediaprocessing.processer.runners.segment

import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.arguments.MpegArgument
import no.iktdev.mediaprocessing.processer.runners.Runner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.segment.Segment
import java.io.File

class SegmentConcatRunner(
    private val segments: List<Segment>,
    private val output: File,
    private val ffmpegInstance: FFmpeg
) : Runner() {

    override suspend fun run(): RunnerResult<ConcatPayload> {

        // 1) Build concat list file
        val listFile = File(output.parentFile, "concat_list.txt")
        listFile.writeText(
            segments.joinToString("\n") { "file '${it.output.absolutePath}'" }
        )

        // 2) Build ffmpeg args
        val args = MpegArgument()
            .inputFile(listFile.absolutePath)
            .outputFile(output.absolutePath)
            .args(listOf("-y", "-f", "concat", "-safe", "0", "-c", "copy"))
            .withProgress(false)

        // 3) Run ffmpeg
        ffmpegInstance.run(args)
        val result = ffmpegInstance.result

        return if (result.resultCode == 0) {
            RunnerResult.Success(
                ConcatPayload(
                    output = output,
                    logFile = ffmpegInstance.logFile
                )
            )
        } else {
            RunnerResult.Reject("Concat failed with code ${result.resultCode}")
        }
    }

    data class ConcatPayload(
        val output: File,
        val logFile: File?
    )
}
