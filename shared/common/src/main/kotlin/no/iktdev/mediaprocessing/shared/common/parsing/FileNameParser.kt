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
        cleanedFileName = removeYear(cleanedFileName)
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
        return if (Regexes.season.containsMatchIn(desiredFileName)) {
            Regexes.season.split(desiredFileName).firstOrNull()?.trim() ?: desiredFileName
        } else {
            val result = if (desiredFileName.contains(" - ")) {
                desiredFileName.split(" - ").firstOrNull() ?: desiredFileName
            } else desiredFileName
            result.trim()
        }.trim('.', '-')
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
    fun removeResolutionAndTags(input: String): String {
        var text = Regex("(?i)(\\d+[pk]\\b|(hd|uhd))", RegexOption.IGNORE_CASE).replace(input, " ")
        text = Regex("(?i)(\\s(bluray|laserdisc|dvd|web))", RegexOption.IGNORE_CASE).replace(text, " ")
        return text
    }


    fun removeYear(text: String): String {
        val match = Regex("\\b\\d{4}\\W").find(text, 0)?.value
        if (match == null || text.indexOf(match) > 0) {
            //return Regex("\\b\\d{4}\\b(.*)").replace(text, " ")
            return Regex("\\b\\d{4}\\b").replace(text, "")
        }
        return text
    }

    fun removeDot(input: String): String {
        //var text = Regex("(?<=\\s)\\.|\\.(?=\\s)").replace(input, "")
        //return Regex("\\.(?<!(Dr|Mr|Ms|Mrs|Lt|Capt|Prof|St|Ave)\\.)\\b").replace(text, " ")
        return Regex("(?<!\\b(?:Dr|Mr|Ms|Mrs|Lt|Capt|Prof|St|Ave))\\.+(?=\\s|\\w)").replace(input, " ")
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