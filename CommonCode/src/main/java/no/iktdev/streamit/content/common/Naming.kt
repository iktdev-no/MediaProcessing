package no.iktdev.streamit.content.common

class Naming(val fileName: String) {
    var cleanedFileName: String
        private set

    init {
        cleanedFileName = fileName.apply {
            removeBracketedText(this)
            removeParenthesizedText(this)
            removeResolutionAndTags(this)
            removeInBetweenCharacters(this)

            removeExtraWhiteSpace(this)
        }
    }

    fun guessDesiredFileName(): String {
        val parts = fileName.split(" - ")
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

            else -> fileName
        }
    }

    fun guessDesiredTitle(): String {
        val desiredFileName = guessDesiredFileName()
        return if (desiredFileName.contains(" - ")) {
            return desiredFileName.split(" - ").firstOrNull() ?: desiredFileName
        } else desiredFileName
    }


    /**
     * Checks whether the filename contains the keyword movie, if so, default to movie
     */
    fun doesContainMovieKeywords(): Boolean {
        return getMatch("[(](?<=\\()movie(?=\\))[)]")?.isBlank() ?: false
    }

    /**
     * @return not null if matches "S01E01"
     */
    fun isSeasonEpisodeDefined(): String? {
        return getMatch("(?i)S[0-9]+E[0-9]+(?i)")
    }

    /**
     * @return not null if matches " 2020 " or ".2020."
     */
    fun isDefinedWithYear(): String? {
        return getMatch("[ .][0-9]{4}[ .]")
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


    private fun getMatch(regex: String): String? {
        return Regex(regex).find(fileName)?.value ?: return null
    }

}