package no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference

data class LanguagePreference(
    val preferredAudio: List<String>,
    val preferredSubtitles: List<String>,

    val preferOriginal: Boolean = true,
    val avoidDub: Boolean = true,

    // NEW: Prioritet for hvilket subtitle-format som skal brukes som master
    val subtitleFormatPriority: List<String> = listOf("ass", "srt", "vtt", "smi"),

    // NEW: Hvordan subtitles skal velges
    val subtitleSelectionMode: SubtitleSelectionMode = SubtitleSelectionMode.DialogueOnly
) {
    companion object {
        fun default() = LanguagePreference(
            preferredAudio = listOf("eng", "nor", "jpn"),
            preferredSubtitles = listOf("eng", "nor"),
            preferOriginal = true,
            avoidDub = true,
            subtitleFormatPriority = listOf("ass", "srt", "vtt", "smi"),
            subtitleSelectionMode = SubtitleSelectionMode.DialogueOnly
        )
    }
}