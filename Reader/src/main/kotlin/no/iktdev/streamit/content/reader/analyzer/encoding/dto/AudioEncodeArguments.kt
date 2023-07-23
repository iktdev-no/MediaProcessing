package no.iktdev.streamit.content.reader.analyzer.encoding.dto

import no.iktdev.streamit.content.common.streams.AudioStream
import no.iktdev.streamit.content.reader.preference

class AudioEncodeArguments(val audio: AudioStream, val index: Int) {

    fun isAudioCodecEqual() = audio.codec_name.lowercase() == preference.audio.codec.lowercase()

    fun shouldUseEAC3(): Boolean {
        return (preference.audio.defaultToEAC3OnSurroundDetected && audio.channels > 2 && audio.codec_name.lowercase() != "eac3")
    }

    fun getAudioArguments(): MutableList<String> {
        val result = mutableListOf<String>()
        if (shouldUseEAC3()) {
            result.addAll(listOf("-c:a", "eac3"))
        } else if (!isAudioCodecEqual()) {
            result.addAll(listOf("-c:a", preference.audio.codec))
        } else result.addAll(listOf("-acodec", "copy"))
        result.addAll(listOf("-map", "0:a:${index}"))
        return result
    }
}