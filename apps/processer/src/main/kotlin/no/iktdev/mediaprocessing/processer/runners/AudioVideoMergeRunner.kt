package no.iktdev.mediaprocessing.processer.runners

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg

class AudioVideoMergeRunner(
    private val videoFile: IFile,
    private val audioFiles: List<AudioEncodeRunner.AudioEncodePayload>,
    private val output: IFile,
    private val ffmpegInstance: FFmpeg
) : Runner() {

    override suspend fun run(): RunnerResult<MergePayload> {

        val dsl = ffmpeg {

            // VIDEO INPUT
            input(videoFile.absolutePath) {
                video(0) {
                    map = true
                    codec = VideoCodec.Copy
                }
            }

            // AUDIO INPUTS
            audioFiles.forEachIndexed { index, encoded ->
                input(encoded.output.absolutePath) {
                    audio(0) {
                        map = true
                        codec = AudioCodec.Copy

                        // metadata
                        language = encoded.meta.language
                        title = encoded.meta.title
                        default = encoded.meta.default
                        forced = encoded.meta.forced
                        commentary = encoded.meta.commentary
                        descriptive = encoded.meta.descriptive
                        hearingImpaired = encoded.meta.hearingImpaired
                        original = encoded.meta.original
                    }
                }
            }

            // OUTPUT
            output(output.absolutePath) {
                overwrite = true
                progress = false
                useWorkFile = true
            }
        }

        ffmpegInstance.run(dsl)
        val result = ffmpegInstance.result

        return if (result.resultCode == 0) {
            RunnerResult.Success(MergePayload(output))
        } else {
            RunnerResult.Reject("Final merge failed with code ${result.resultCode}")
        }
    }

    data class MergePayload(val output: IFile)
}
