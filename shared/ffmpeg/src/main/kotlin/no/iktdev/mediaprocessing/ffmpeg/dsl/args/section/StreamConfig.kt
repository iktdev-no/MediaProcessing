package no.iktdev.mediaprocessing.ffmpeg.dsl.args.section

import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec

open class StreamConfig(
    val type: StreamType,
    val streamIndex: Int,
    var map: Boolean = false
    ) {
    /**
     * If true, this stream will emit an explicit -map for this input/stream.
     * If no streams anywhere have map = true, compiler will emit NO -map at all,
     * and FFmpeg will use its default auto-mapping (inkl. default audio).
     */
}

class VideoStreamConfig(
    streamIndex: Int,
    var codec: VideoCodec? = null,
    val options: VideoCodecOptions = VideoCodecOptions(),
    map: Boolean = false
) : StreamConfig(StreamType.VIDEO, streamIndex = streamIndex)


class AudioStreamConfig(
    streamIndex: Int,
    var codec: AudioCodec? = null,
    val options: AudioCodecOptions = AudioCodecOptions(),
    var language: String? = null,
    var title: String? = null,
    var default: Boolean = false,
    var forced: Boolean = false,
    var commentary: Boolean = false,
    var descriptive: Boolean = false,
    var hearingImpaired: Boolean = false,
    var original: Boolean = false,
) : StreamConfig(StreamType.AUDIO, streamIndex = streamIndex) {}

class SubtitleStreamConfig(
    streamIndex: Int,
    var language: String? = null,
    var title: String? = null,
    var forced: Boolean = false,
) : StreamConfig(StreamType.SUBTITLE, streamIndex = streamIndex, map = true) {
    val codec: String = "copy"
}



class VideoCodecOptions {
    var crf: Int? = null
    var preset: String? = null
    var tune: String? = null
    var profile: String? = null
    var level: String? = null
    var bitrate: String? = null
    var maxrate: String? = null
    var bufsize: String? = null
    var pixFmt: String? = null
    var fps: Int? = null
    val filters: MutableList<String> = mutableListOf()
}

class AudioCodecOptions {
    var bitrate: String? = null
    var channels: Int? = null
    var sampleRate: Int? = null
    var channelLayout: String? = null
    val filters: MutableList<String> = mutableListOf()
}
