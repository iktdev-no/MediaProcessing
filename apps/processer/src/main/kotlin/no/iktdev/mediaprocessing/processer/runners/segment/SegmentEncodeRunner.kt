package no.iktdev.mediaprocessing.processer.runners.segment

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.processer.runners.Runner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.processors.segment.Segment

class SegmentEncodeRunner(
    private val segment: Segment,
    private val videoInstructions: FFmpegInstructions,
    private val ffmpegInstance: FFmpeg
) : Runner() {

    fun useWorkFileForSegments(): Boolean {
        return true
    }

    override suspend fun run(): RunnerResult<SegmentEncodePayload> {

        val dsl = ffmpeg {
            fromInstructions(videoInstructions)
            segment(segment.start, segment.duration)
            output(segment.output.absolutePath) {
                overwrite = true
                progress = true
                useWorkFile = useWorkFileForSegments()
            }
        }

        ffmpegInstance.run(dsl)
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
        val output: IFile
    )

}
