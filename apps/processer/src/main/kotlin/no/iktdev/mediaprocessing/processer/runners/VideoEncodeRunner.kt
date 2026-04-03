package no.iktdev.mediaprocessing.processer.runners

import mu.KotlinLogging
import no.iktdev.eventi.models.Task
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.processer.models.ProcessEntry
import no.iktdev.mediaprocessing.processer.models.ProcessType
import no.iktdev.mediaprocessing.processer.services.ProcessService
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class VideoEncodeRunner(
    private val taskId: UUID,
    private val videoInstructions: FFmpegInstructions,
    private val outputDirectory: IFile,
    private val outputFile: IFile,
    private val ffmpegInstance: FFmpeg,
    private val processService: ProcessService? = null
): Runner() {
    private val log = KotlinLogging.logger {}

    override suspend fun run(): RunnerResult<VideoEncodeResult> {
        var _pid: Long? = null

        val dsl = ffmpeg {
            fromInstructions(videoInstructions)
            outputDirectory(outputDirectory)
            output(outputFile.name) {
                progress = true
                useWorkFile = true
                overwrite = videoInstructions.output?.overwrite ?: false
            }
        }

        ffmpegInstance.run(dsl) { pid ->
            _pid = pid

            log.warn { "VideoEncodeRunner.run() with pid '$pid'" }
            pid.let { pid ->
                processService?.addProcess(ProcessEntry(pid, taskId, ProcessType.LINEAR_VIDEO_ENCODE))
            }
        }
        val result = ffmpegInstance.result
        if (!outputFile.exists()) {
            return RunnerResult.Reject("Attempted to verify placement of ${dsl.outputFile()} using ${outputFile.absolutePath}, but it was not in expected folder ${outputDirectory.absolutePath}")
        }

        _pid?.let { processService?.removeProcess(it) }
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