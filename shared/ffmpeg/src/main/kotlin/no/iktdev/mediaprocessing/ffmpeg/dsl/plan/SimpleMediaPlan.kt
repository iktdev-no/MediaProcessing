package no.iktdev.mediaprocessing.ffmpeg.dsl.plan

import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.ffmpeg.model.AudioTarget
import no.iktdev.mediaprocessing.ffmpeg.model.EncodeStrategy
import no.iktdev.mediaprocessing.ffmpeg.model.VideoTarget
import no.iktdev.mediaprocessing.ffmpeg.util.FfmpegCodecs

class SimpleMediaPlan(sourceVideoCodec: FfmpegCodecs, videoTrack: VideoTarget, audioTracks: List<AudioTarget>, val encodeStrategy: EncodeStrategy) :
    BaseMediaPlan(sourceVideoCodec, videoTrack, audioTracks) {

    fun toVideoInstructions(inputFile: String, outputFile: String): FFmpegInstructions {

        val dsl = ffmpeg {

            input(inputFile) {
                video(videoTrack.listIndex) {
                    map = true
                    codec = videoTrack.codec
                }
            }

            output(outputFile) {
                progress = encodeStrategy == EncodeStrategy.Linear
            }
        }

        return dsl.toInstructions()
    }

    fun toAudioInstructions(inputFile: String): List<FFmpegInstructions> =
        getUsableAudioTargetedTracks().mapIndexed { idx, target ->

            val dsl = ffmpeg {

                input(inputFile) {
                    audio(target.listIndex) {
                        map = true
                        codec = target.codec
                        language = target.meta?.language
                    }
                }

                output("audio_track_$idx.mka") {
                    progress = encodeStrategy == EncodeStrategy.Linear
                }
            }

            dsl.toInstructions()
        }
}
