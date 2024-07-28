package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams

import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.SubtitleArgumentsDto
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.SubtitleStream

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
        val title = this.tags.title?.lowercase() ?: return false
        val keywords = listOf("cc", "closed caption")
        return keywords.any { title.contains(it) }
    }

    private fun SubtitleStream.isSHD(): Boolean {
        val title = this.tags.title?.lowercase() ?: return false
        val keywords = listOf("shd", "hh", "Hard-of-Hearing", "Hard of Hearing")
        return keywords.any { title.contains(it) }
    }

    private fun SubtitleStream.isSignOrSong(): Boolean {
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
        val mappedToType =
            codecFiltered.map { getSubtitleType(it) to it }.filter { it.first in SubtitleType.entries }
                .groupBy { it.second.tags.language ?: "eng" }
                .mapValues { entry ->
                    val languageStreams = entry.value
                    val sortedStreams = languageStreams.sortedBy { SubtitleType.entries.indexOf(it.first) }
                    sortedStreams.firstOrNull()?.second
                }.mapNotNull { it.value }

        return mappedToType.mapNotNull { stream ->
            getFormatToCodec(stream.codec_name)?.let { format ->
                SubtitleArgumentsDto(
                    index = subtitleStreams.indexOf(stream),
                    language = stream.tags.language ?: "eng",
                    format = format
                )
            }
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