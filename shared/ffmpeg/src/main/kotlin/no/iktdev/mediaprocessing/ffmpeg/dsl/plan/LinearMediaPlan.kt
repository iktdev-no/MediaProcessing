package no.iktdev.mediaprocessing.ffmpeg.dsl.plan

import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.ffmpeg.model.AudioTarget
import no.iktdev.mediaprocessing.ffmpeg.model.VideoTarget

class LinearMediaPlan(videoTrack: VideoTarget, audioTracks: List<AudioTarget>) :
    BaseMediaPlan(videoTrack, audioTracks) {

    fun toInstructions(inputFile: String, outputFile: String): FFmpegInstructions {

        val dsl = ffmpeg {

            input(inputFile) {

                // VIDEO STREAM
                video(videoTrack.listIndex) {
                    map = true
                    codec = videoTrack.codec
                }

                // AUDIO STREAMS
                getUsableAudioTargetedTracks().forEach { t ->
                    audio(t.listIndex) {
                        map = true
                        codec = t.codec
                    }
                }
            }

            output(outputFile)
        }

        return dsl.toInstructions()
    }
}
