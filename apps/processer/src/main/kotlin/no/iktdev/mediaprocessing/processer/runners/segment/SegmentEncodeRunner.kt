package no.iktdev.mediaprocessing.processer.runners.segment

import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.arguments.MpegArgument
import no.iktdev.mediaprocessing.processer.runners.Runner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.segment.Segment
import java.io.File

class SegmentEncodeRunner(
    private val segment: Segment,
    private val input: File,
    private val args: List<String>,
    private val ffmpegInstance: FFmpeg
) : Runner() {


    override suspend fun run(): RunnerResult<SegmentEncodePayload> {

        val segmentArgs = MpegArgument()
            .inputFile(input.absolutePath)
            .outputFile(segment.output.absolutePath)
            .args(
                listOf(
                    "-y",
                    "-ss", segment.start.toString(),
                    "-t", segment.duration.toString()
                ) + args
            )
            .withProgress(true)

        ffmpegInstance.run(segmentArgs)
        val result = ffmpegInstance.result

        return if (result.resultCode == 0) {
            RunnerResult.Success(
                SegmentEncodePayload(
                    index = segment.index,
                    output = segment.output
                )
            )
        } else {
            RunnerResult.Reject(
                "Segment ${segment.index} failed with code ${result.resultCode}"
            )
        }
    }

    data class SegmentEncodePayload(
        val index: Int,
        val output: File
    )

}
