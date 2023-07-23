package no.iktdev.streamit.content.reader.analyzer.encoding.dto

import no.iktdev.streamit.content.common.streams.SubtitleStream

class SubtitleEncodeArguments(val subtitle: SubtitleStream, val index: Int) {

    fun getSubtitleArguments(): List<String> {
        val result = mutableListOf<String>()
        result.addAll(listOf("-c:s", "copy"))
        result.addAll(listOf("-map", "0:s:$index"))
        return result
    }

}