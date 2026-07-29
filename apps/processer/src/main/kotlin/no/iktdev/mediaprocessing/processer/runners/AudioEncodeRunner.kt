package no.iktdev.mediaprocessing.processer.runners

import mu.KotlinLogging
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.AudioStreamConfig
import no.iktdev.mediaprocessing.ffmpeg.util.getAudioMetadata
import no.iktdev.mediaprocessing.shared.common.dto.processer.ProcessEntry
import no.iktdev.mediaprocessing.shared.common.dto.processer.ProcessType
import no.iktdev.mediaprocessing.processer.services.ProcessService
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class AudioEncodeRunner(
    private val taskId: UUID,
    private val audioInstruction: FFmpegInstructions,
    private val outputDirectory: IFile,
    private val outputFile: IFile,
    private val ffmpegInstance: FFmpeg
): Runner() {
    @Autowired
    private var processService: ProcessService? = null
    private val log = KotlinLogging.logger {}

    override suspend fun run(): RunnerResult<AudioEncodePayload> {
        var _pid: Long? = null
        val dsl = ffmpeg {
            fromInstructions(audioInstruction)
            outputDirectory(outputDirectory)
        }

        ffmpegInstance.run(dsl, onPid = { pid ->
             _pid = pid
            log.warn { "AudioEncodeRunner.run() with pid '$pid'" }
            pid.let { pid ->
                processService?.addProcess(ProcessEntry(pid, taskId, ProcessType.SEGMENTED_VIDEO_ENCODE))
            }})
        val result = ffmpegInstance.result
        if (!outputFile.exists()) {
            return RunnerResult.Reject("Attempted to verify placement of ${dsl.outputFile()} using ${outputFile.absolutePath}, but it was not in expected folder ${outputDirectory.absolutePath}")
        }
        _pid?.let { processService?.removeProcess(it) }
        return if (result.resultCode == 0) {
            RunnerResult.Success(AudioEncodePayload(outputFile,
                logFile = ffmpegInstance.logFile,
                audioInstruction.getAudioMetadata()))
        } else {
            RunnerResult.Reject("Audio encode failed with code ${result.resultCode}")
        }
    }
    data class AudioEncodePayload(
        val output: IFile,
        val logFile: IFile? = null,
        val meta: AudioStreamConfig
    )




}