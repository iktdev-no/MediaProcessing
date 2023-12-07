package no.iktdev.mediaprocessing.shared.common.extended

import java.io.File

val validVideoFiles = listOf(
    "mkv",
    "avi",
    "mp4",
    "wmv",
    "webm",
    "mov"
)

fun File.isSupportedVideoFile(): Boolean {
    return this.isFile && validVideoFiles.contains(this.extension)
}

fun getSanitizedFileName(name: String): String {
    /**
     * Modifies the input value and removes "[Text]"
     * @param text "[TEST] Dummy - 01 [AZ 1080p] "
     */
    fun removeBracketedText(text: String): String {
        return Regex("\\[.*?]").replace(text, " ")
    }

    /**
     *
     */
    fun removeParenthesizedText(text: String): String {
        return Regex("\\(.*?\\)").replace(text, " ")
    }

    /**
     *
     */
    fun removeResolutionAndTags(text: String): String {
        return Regex("(.*?)(?=\\d+[pk]\\b)").replace(text, " ")
    }

    fun removeInBetweenCharacters(text: String): String {
        return Regex("[.]").replace(text, " ")
    }

    /**
     * @param text "example    text   with  extra   spaces"
     * @return example text with extra spaces
     */
    fun removeExtraWhiteSpace(text: String): String {
        return Regex("\\s{2,}").replace(text, " ")
    }

    return name
        .let { removeBracketedText(it) }
        .let { removeParenthesizedText(it) }
        .let { removeResolutionAndTags(it) }
        .let { removeInBetweenCharacters(it) }
        .let { removeExtraWhiteSpace(it) }
}


fun File.getDesiredVideoFileName(): String? {
    if (!this.isSupportedVideoFile()) return null
    val cleanedFileName = getSanitizedFileName(this.nameWithoutExtension)
    val parts = cleanedFileName.split(" - ")
    return when {
        parts.size == 2 && parts[1].matches(Regex("\\d{4}")) -> {
            val title = parts[0]
            val year = parts[1]
            "$title ($year)"
        }

        parts.size >= 3 && parts[1].matches(Regex("S\\d+")) && parts[2].matches(Regex("\\d+[vV]\\d+")) -> {
            val title = parts[0]
            val episodeWithRevision = parts[2]
            val episodeParts = episodeWithRevision.split("v", "V")
            val episodeNumber = episodeParts[0].toInt()
            val revisionNumber = episodeParts[1].toInt()
            val seasonEpisode =
                "S${episodeNumber.toString().padStart(2, '0')}E${revisionNumber.toString().padStart(2, '0')}"
            val episodeTitle = if (parts.size > 3) parts[3] else ""
            "$title - $seasonEpisode - $episodeTitle"
        }

        else -> cleanedFileName
    }.trim()
}

fun File.getGuessedVideoTitle(): String? {
    val desiredFileName = getDesiredVideoFileName() ?: return null
    val seasonRegex = Regex("\\sS[0-9]+(\\s- [0-9]+|\\s[0-9]+)", RegexOption.IGNORE_CASE)
    if (seasonRegex.containsMatchIn(desiredFileName)) {
        return seasonRegex.replace(desiredFileName, "").trim()
    } else {
        val result = if (desiredFileName.contains(" - ")) {
            return desiredFileName.split(" - ").firstOrNull() ?: desiredFileName
        } else desiredFileName
        return result.trim()
    }
}