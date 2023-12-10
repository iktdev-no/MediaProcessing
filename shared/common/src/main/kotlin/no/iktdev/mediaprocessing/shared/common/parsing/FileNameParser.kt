package no.iktdev.mediaprocessing.shared.common.parsing

class FileNameParser(val fileName: String) {
    var cleanedFileName: String
        private set

    init {
        cleanedFileName = removeBracketedText(fileName)
        cleanedFileName = removeParenthesizedText(cleanedFileName)
        cleanedFileName = removeResolutionAndTrailing(cleanedFileName)
        cleanedFileName = removeResolutionAndTags(cleanedFileName)
        cleanedFileName = removeParenthesizedText(cleanedFileName)
        cleanedFileName = removeYearAndTrailing(cleanedFileName)
        cleanedFileName = removeDot(cleanedFileName)
        cleanedFileName = removeExtraWhiteSpace(cleanedFileName)
        cleanedFileName = removeTrailingAndLeadingCharacters(cleanedFileName).trim()

    }

    fun guessDesiredFileName(): String {
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

    fun guessDesiredTitle(): String {
        val desiredFileName = guessDesiredFileName()
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

    fun removeResolutionAndTrailing(text: String): String {
        return Regex("[0-9]+[pP].*").replace(text, "")
    }

    fun removeTrailingAndLeadingCharacters(text: String): String {
        return Regex("^[^a-zA-Z0-9!,]+|[^a-zA-Z0-9!~,]+\$").replace(text, " ")
    }

    /**
     *
     */
    fun removeResolutionAndTags(text: String): String {
        return Regex("(.*?)(?=\\d+[pk]\\b)").replace(text, " ")
    }


    fun removeYearAndTrailing(text: String): String {
        val match = Regex("\\b\\d{4}\\W").find(text, 0)?.value
        if (match == null || text.indexOf(match) > 0) {
            return Regex("\\b\\d{4}\\b(.*)").replace(text, " ")
        }
        return text
    }

    fun removeDot(text: String): String {
        return Regex("\\.(?<!(Dr|Mr|Ms|Mrs|Lt|Capt|Prof|St|Ave)\\.)\\b").replace(text, " ")
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


    private fun getMatch(regex: String): String? {
        return Regex(regex).find(fileName)?.value ?: return null
    }

}