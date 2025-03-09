package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams

import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.SubtitleArgumentsDto
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.SubtitleStream
import kotlin.math.sqrt

class SubtitleArguments(val subtitleStreams: List<SubtitleStream>) {
    /**
     * @property DEFAULT is default subtitle as dialog
     * @property CC is Closed-Captions
     * @property SHD is Hard of hearing
     * @property NON_DIALOGUE is for Signs or Song (as in lyrics)
     */
    private enum class SubtitleType {
        DEFAULT,
        CC,
        SHD,
        NON_DIALOGUE
    }

    private fun SubtitleStream.isCC(): Boolean {
        if ((this.disposition?.captions ?: 0) > 0) {
            return true
        }
        val title = this.tags.title?.lowercase() ?: return false
        val keywords = listOf("cc", "closed caption")
        return keywords.any { title.contains(it) }
    }

    private fun SubtitleStream.isSHD(): Boolean {
        if ((this.disposition?.hearing_impaired ?: 0) > 0) {
            return true
        }
        val title = this.tags.title?.lowercase() ?: return false
        val keywords = listOf("shd", "hh", "Hard-of-Hearing", "Hard of Hearing")
        return keywords.any { title.contains(it) }
    }

    private fun SubtitleStream.isSignOrSong(): Boolean {
        if ((this.disposition?.lyrics ?: 0) > 0) {
            return true
        }
        val title = this.tags.title?.lowercase() ?: return false
        val keywords = listOf("song", "songs", "sign", "signs")
        return keywords.any { title.contains(it) }
    }

    private fun getSubtitleType(stream: SubtitleStream): SubtitleType {
        return if (stream.isSignOrSong())
            SubtitleType.NON_DIALOGUE
        else if (stream.isSHD()) {
            SubtitleType.SHD
        } else if (stream.isCC()) {
            SubtitleType.CC
        } else SubtitleType.DEFAULT
    }

    fun getSubtitleArguments(): List<SubtitleArgumentsDto> {
        val acceptable = subtitleStreams.filter { !it.isSignOrSong() }
        val codecFiltered = acceptable.filter { getFormatToCodec(it.codec_name) != null }

        val languageGrouped = codecFiltered.groupBy { it.tags.language ?: "eng" }

        val streamsToExtract = languageGrouped.mapNotNull { item ->
            val itemToType = item.value.map { it to getSubtitleType(it) }
            val usableSubtitles = itemToType.filter { it.second == SubtitleType.DEFAULT }.ifEmpty { itemToType }
            val excludedLowFrameCount = excludeLowFrameCount(usableSubtitles.map { it.first }).sortedByDescending { it.tags.NUMBER_OF_FRAMES }
            excludedLowFrameCount.firstOrNull() ?: run {
                usableSubtitles.map { it.first }.firstOrNull { it.disposition?.default == 1 } ?: usableSubtitles.firstOrNull()?.first
            }
        }

        return streamsToExtract.mapNotNull { stream ->
            getFormatToCodec(stream.codec_name)?.let { format ->
                SubtitleArgumentsDto(
                    mediaIndex = stream.index,
                    index = subtitleStreams.indexOf(stream),
                    language = stream.tags.language ?: "eng",
                    format = format
                )
            }
        }

    }

    fun excludeLowFrameCount(streams: List<SubtitleStream>): List<SubtitleStream> {
        val usable = streams.filter { (it.tags.NUMBER_OF_FRAMES ?: 0) > 0 }
        val mean = usable.mapNotNull { it.tags.NUMBER_OF_FRAMES }.average()
        val variance = usable.map { (it.tags.NUMBER_OF_FRAMES!! - mean) * (it.tags.NUMBER_OF_FRAMES!! - mean) }.average()
        val standardDeviation = sqrt(variance)

        // Definer intervallet for "normale" rammer: mean ± 2 * standard deviation
        val lowerBound = mean - 2 * standardDeviation
        val upperBound = mean + 2 * standardDeviation

        return usable.filter {
            val frameCount = it.tags.NUMBER_OF_FRAMES ?: 0
            frameCount.toDouble() in lowerBound..upperBound
        }
    }

    fun getFormatToCodec(codecName: String): String? {
        return when (codecName) {
            "ass" -> "ass"
            "subrip" -> "srt"
            "webvtt", "vtt" -> "vtt"
            "smi" -> "smi"
            "hdmv_pgs_subtitle" -> null
            else -> null
        }
    }

}