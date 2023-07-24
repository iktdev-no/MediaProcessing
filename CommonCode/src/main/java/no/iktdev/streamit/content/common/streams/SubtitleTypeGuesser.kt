package no.iktdev.streamit.content.common.streams

/**
 * @property SHD is Hard of hearing
 * @property CC is Closed-Captions
 * @property NON_DIALOGUE is for Signs or Song (as in lyrics)
 * @property DEFAULT is default subtitle as dialog
 */
enum class SubtitleType {
    SHD,
    CC,
    NON_DIALOGUE,
    DEFAULT
}

class SubtitleTypeGuesser {
    fun guessType(subtitle: SubtitleStream): SubtitleType {
        if (subtitle.tags != null && subtitle.tags.title?.isBlank() == false) {
            val title = subtitle.tags.title!!
            if (title.lowercase().contains("song")
                || title.lowercase().contains("songs")
                || title.lowercase().contains("sign")
                || title.lowercase().contains("signs")
            ) {
                return SubtitleType.NON_DIALOGUE
            }
            if (getSubtitleType(title, listOf("cc", "closed caption"),
                    SubtitleType.CC
                ) == SubtitleType.CC
            ) return SubtitleType.CC
            if (getSubtitleType(title, listOf("shd", "hh", "Hard-of-Hearing", "Hard of Hearing"),
                    SubtitleType.SHD
                ) == SubtitleType.SHD
            ) return SubtitleType.SHD
        }

        return SubtitleType.DEFAULT
    }

    private fun getSubtitleType(title: String, keys: List<String>, expected: SubtitleType): SubtitleType {
        val bracedText = Regex.fromLiteral("[(](?<=\\().*?(?=\\))[)]").find(title)
        val brakedText = Regex.fromLiteral("[(](?<=\\().*?(?=\\))[)]").find(title)

        if (bracedText == null || brakedText == null)
            return SubtitleType.DEFAULT

        var text = bracedText.value.ifBlank { brakedText.value }
        text = Regex.fromLiteral("[\\[\\]()-.,_+]").replace(text, "")

        return if (keys.find { item ->
                item.lowercase().contains(text.lowercase()) || text.lowercase().contains(item.lowercase())
            }.isNullOrEmpty()) SubtitleType.DEFAULT else expected

    }
}

