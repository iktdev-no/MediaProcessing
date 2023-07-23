package no.iktdev.streamit.content.common.streams

class SubtitleStreamSelector(val streams: List<SubtitleStream>) {

    fun getDesiredStreams(): List<SubtitleStream> {
        val codecFiltered = streams.filter { getFormatToCodec(it.codec_name) != null }
        // TODO: Expand and remove stuff like sign and songs etc..
        return codecFiltered
    }


    fun getFormatToCodec(codecName: String): String? {
        return when(codecName) {
            "ass" -> "ass"
            "subrip" -> "srt"
            "webvtt", "vtt" -> "vtt"
            "smi" -> "smi"
            "hdmv_pgs_subtitle" -> null
            else -> null
        }
    }
}