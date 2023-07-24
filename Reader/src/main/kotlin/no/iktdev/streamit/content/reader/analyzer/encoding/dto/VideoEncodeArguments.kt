package no.iktdev.streamit.content.reader.analyzer.encoding.dto

import no.iktdev.streamit.content.common.streams.VideoStream
import no.iktdev.streamit.content.reader.preference

class VideoEncodeArguments(val video: VideoStream, val index: Int) {

    fun isVideoCodecEqual() = getCodec(video.codec_name) == getCodec(preference.video.codec.lowercase())


    fun getVideoArguments(): List<String> {
        val result = mutableListOf<String>()
        if (isVideoCodecEqual()) result.addAll(listOf(
            "-vcodec", "copy"
        )) else {
            result.addAll(listOf("-c:v", getCodec(preference.video.codec.lowercase())))
            result.addAll(listOf("-crf", preference.video.threshold.toString()))
        }
        if (preference.video.pixelFormatPassthrough.none { it == video.pix_fmt }) {
            result.addAll(listOf("-pix_fmt", preference.video.pixelFormat))
        }
        result.addAll(listOf("-map", "0:v:${index}"))
        return result
    }


    protected fun getCodec(name: String): String {
        return when(name) {
            "hevc", "hevec", "h265", "h.265", "libx265"
                -> "libx265"
            "h.264", "h264", "libx264"
                -> "libx264"
            else -> name
        }
    }
}