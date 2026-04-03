package no.iktdev.mediaprocessing.processer.runners.segment

import mu.KotlinLogging
import no.iktdev.eventi.models.Task
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.processer.models.ProcessEntry
import no.iktdev.mediaprocessing.processer.models.ProcessType
import no.iktdev.mediaprocessing.processer.runners.Runner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.processors.segment.Segment
import no.iktdev.mediaprocessing.processer.services.ProcessService
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class SegmentEncodeRunner(
    private val taskId: UUID,
    private val segment: Segment,
    private val videoInstructions: FFmpegInstructions,
    private val ffmpegInstance: FFmpeg,
    private val processService: ProcessService? = null
) : Runner() {
    @Autowired
    private val log = KotlinLogging.logger {}

    fun useWorkFileForSegments(): Boolean {
        return true
    }

    override suspend fun run(): RunnerResult<SegmentEncodePayload> {
        var _pid: Long? = null

        val dsl = ffmpeg {
            fromInstructions(videoInstructions)
            segment(segment.start, segment.duration)
            output(segment.output.absolutePath) {
                overwrite = true
                progress = true
                useWorkFile = useWorkFileForSegments()
            }
        }

        ffmpegInstance.run(dsl, onPid = { pid ->
            _pid = pid
            log.warn { "SegmentEncodeRunner.run() with pid '$pid'" }
            pid.let { pid ->
                processService?.addProcess(ProcessEntry(pid, taskId, ProcessType.SEGMENTED_VIDEO_ENCODE))
            }
        })
        val result = ffmpegInstance.result
        _pid?.let { processService?.removeProcess(it) }
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
