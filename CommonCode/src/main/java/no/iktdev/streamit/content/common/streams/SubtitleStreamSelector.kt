package no.iktdev.streamit.content.common.streams

class SubtitleStreamSelector(val streams: List<SubtitleStream>) {

    fun getCandidateForConversion(): List<SubtitleStream> {
        val languageGrouped = getDesiredStreams().groupBy { it.tags.language ?: "eng" }
        val priority = listOf("subrip", "srt", "webvtt", "vtt", "ass")

        val result = mutableListOf<SubtitleStream>()
        for ((language, streams) in languageGrouped) {
            val selectedStream = streams.firstOrNull { it.codec_name in priority }
            if (selectedStream != null) {
                result.add(selectedStream)
            }
        }
        return result
    }

    fun getDesiredStreams(): List<SubtitleStream> {
        val desiredTypes = listOf(SubtitleType.DEFAULT, SubtitleType.CC, SubtitleType.SHD)
        val typeGuesser = SubtitleTypeGuesser()
        val codecFiltered = streams.filter { getFormatToCodec(it.codec_name) != null }

        val mappedToType = codecFiltered.map { typeGuesser.guessType(it) to it }.filter { it.first in desiredTypes }
            .groupBy { it.second.tags.language ?: "eng" }
            .mapValues { entry ->
                val languageStreams = entry.value
                val sortedStreams = languageStreams.sortedBy { desiredTypes.indexOf(it.first) }
                sortedStreams.firstOrNull()?.second
            }.mapNotNull { it.value }


        return mappedToType
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