package no.iktdev.mediaprocessing.processer.runners.segment

import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.processer.runners.Runner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.processors.segment.Segment
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import java.util.UUID

class SegmentConcatRunner(
    private val taskId: UUID,
    private val segments: List<Segment>,
    private val intermediateStore: IFile,
    private val output: IFile,
    private val ffmpegInstance: FFmpeg
) : Runner() {

    override suspend fun run(): RunnerResult<ConcatPayload> {

        // 1) Build concat list file
        val baseOutputName = output.nameWithoutExtension
        val listFile = intermediateStore.using("$baseOutputName - CONCAT_LIST.txt")
        val lines = segments.map { segment ->
            val escaped = segment.output.absolutePath.replace("'", "'\\''")
            "file '$escaped'"
        }

        listFile.writeText(lines.joinToString("\n"))

        val dsl = ffmpeg {
            concatFile(listFile.absolutePath) {
            }

            // output
            output(output.absolutePath) {
                overwrite = true
                useWorkFile = false
            }
        }


        // 3) Run ffmpeg
        ffmpegInstance.run(dsl)
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
        val output: IFile,
        val logFile: IFile?
    )
}
