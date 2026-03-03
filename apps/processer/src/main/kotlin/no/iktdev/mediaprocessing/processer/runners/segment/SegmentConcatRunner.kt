package no.iktdev.mediaprocessing.processer.runners.segment

import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.arguments.MpegArgument
import no.iktdev.mediaprocessing.processer.runners.Runner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.segment.Segment
import java.io.File

class SegmentConcatRunner(
    private val segments: List<Segment>,
    private val intermediateStore: File,
    private val output: File,
    private val ffmpegInstance: FFmpeg
) : Runner() {

    override suspend fun run(): RunnerResult<ConcatPayload> {

        // 1) Build concat list file
        val baseOutputName = output.nameWithoutExtension
        val listFile = intermediateStore.using("$baseOutputName - CONCAT_LIST.txt")
        val lines = segments.map { segment ->
            val escaped = segment.output.absolutePath.replace("'", "\\'")
            "file '$escaped'"
        }

        listFile.writeText(lines.joinToString("\n"))


        // 2) Build ffmpeg args
        val args = MpegArgument()
            .preArgs("-y", "-f", "concat", "-safe", "0")
            .inputFile(listFile.absolutePath)
            .args("-c", "copy")
            .outputFile(output.absolutePath)
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
