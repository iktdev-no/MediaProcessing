package no.iktdev.streamit.content.reader.analyzer.encoding.dto

import no.iktdev.streamit.content.common.streams.SubtitleStream

class SubtitleEncodeArguments(val subtitle: SubtitleStream) {

    fun getSubtitleArguments(): List<String> {
        val result = mutableListOf<String>()
        result.addAll(listOf("-c:s", "copy"))
        result.addAll(listOf("-map", "0:s:${subtitle.index}"))
        return result
    }

}