package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams

import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.VideoArgumentsDto
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.VideoPreference
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.VideoStream

class VideoArguments(
    val videoStream: VideoStream,
    val allStreams: ParsedMediaStreams,
    val preference: VideoPreference
) {
    fun isVideoCodecEqual() = getCodec(videoStream.codec_name) == getCodec(preference.codec.lowercase())
    protected fun getCodec(name: String): String {
        return when (name) {
            "hevc", "hevec", "h265", "h.265", "libx265"
            -> "libx265"

            "h.264", "h264", "libx264"
            -> "libx264"

            else -> name
        }
    }

    fun getVideoArguments(): VideoArgumentsDto {
        val optionalParams = mutableListOf<String>()
        if (preference.pixelFormatPassthrough.none { it == videoStream.pix_fmt }) {
            optionalParams.addAll(listOf("-pix_fmt", preference.pixelFormat))
        }
        val codecParams = if (isVideoCodecEqual()) {
            val default = mutableListOf("-c:v", "copy")
            if (getCodec(videoStream.codec_name) == "libx265") {
                default.addAll(listOf("-vbsf", "hevc_mp4toannexb"))
            }
            default
        }
        else {
            optionalParams.addAll(listOf("-crf", preference.threshold.toString()))
            listOf("-c:v", getCodec(preference.codec.lowercase()))
        }

        return VideoArgumentsDto(
            index = allStreams.videoStream.indexOf(videoStream),
            codecParameters = codecParams,
            optionalParameters = optionalParams
        )
    }
}