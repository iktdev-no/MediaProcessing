package no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator


data class LanguagePreference(
    val preferredAudio: List<String>,
    val preferredSubtitles: List<String>,

    val preferOriginal: Boolean = true,
    val avoidDub: Boolean = true,

    // NEW: Prioritet for hvilket subtitle-format som skal brukes som master
    val subtitleFormatPriority: List<String> = listOf("ass", "srt", "vtt", "smi"),

    // NEW: Hvordan subtitles skal velges
    val subtitleSelectionMode: SubtitleSelectionMode = SubtitleSelectionMode.DialogueOnly
)

enum class SubtitleSelectionMode {
    DialogueOnly,          // Kun dialog
    DialogueAndForced,     // Dialog + forced
    All                    // Alle typer
}