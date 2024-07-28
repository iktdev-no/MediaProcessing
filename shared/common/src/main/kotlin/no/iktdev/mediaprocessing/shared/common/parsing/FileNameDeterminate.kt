package no.iktdev.mediaprocessing.shared.common.parsing

import no.iktdev.mediaprocessing.shared.common.contract.data.EpisodeInfo
import no.iktdev.mediaprocessing.shared.common.contract.data.MediaInfo
import no.iktdev.mediaprocessing.shared.common.contract.data.MovieInfo


class FileNameDeterminate(val title: String, val sanitizedName: String, val ctype: ContentType = ContentType.UNDEFINED)  {

    enum class ContentType {
        MOVIE,
        SERIE,
        UNDEFINED
    }

    fun getDeterminedVideoInfo(): MediaInfo? {
        return when (ctype) {
            ContentType.MOVIE -> determineMovieFileName()
            ContentType.SERIE -> determineSerieFileName()
            ContentType.UNDEFINED -> determineUndefinedFileName()
        }
    }

    private fun determineMovieFileName(): MovieInfo? {
        val movieEx = MovieEx(title, sanitizedName)
        val stripped = when {
            movieEx.isDefinedWithYear() -> sanitizedName.replace(movieEx.yearRegex(), "").trim()
            movieEx.doesContainMovieKeywords() -> sanitizedName.replace(Regex("(?i)\\s*\\(\\s*movie\\s*\\)\\s*"), "").trim()
            else -> sanitizedName
        }
        val nonResolutioned = movieEx.removeResolutionAndBeyond(stripped) ?: stripped
        return MovieInfo(title = cleanup(nonResolutioned), fullName =  cleanup(nonResolutioned))
    }

    private fun determineSerieFileName(): EpisodeInfo? {
        val serieEx = SerieEx(title, sanitizedName)

        val (season, episode) = serieEx.findSeasonAndEpisode(sanitizedName)
        val episodeNumberSingle = serieEx.findEpisodeNumber()

        val seasonNumber = season ?: "1"
        val episodeNumber = episode ?: (episodeNumberSingle ?: return null)
        val seasonEpisodeCombined = serieEx.getSeasonEpisodeCombined(seasonNumber, episodeNumber)
        val episodeTitle = serieEx.findEpisodeTitle()

        val useTitle = if (title == sanitizedName) {
            if (title.contains(" - ")) {
                title.split(" - ").firstOrNull() ?: title
            } else {
                val seasonNumberIndex = if (title.indexOf(seasonNumber) < 0) title.length -1 else title.indexOf(seasonNumber)
                val episodeNumberIndex = if (title.indexOf(episodeNumber) < 0) title.length -1 else title.indexOf(episodeNumber)
                val closest = listOf<Int>(seasonNumberIndex, episodeNumberIndex).min()
                val shrunkenTitle = title.substring(0, closest)
                if (closest - shrunkenTitle.lastIndexOf(" ") < 3) {
                    title.substring(0, shrunkenTitle.lastIndexOf(" "))
                } else title.substring(0, closest)

            }
        } else title
        val fullName = "${useTitle.trim()} - $seasonEpisodeCombined ${if (episodeTitle.isNullOrEmpty()) "" else " - $episodeTitle"}".trim()
        return EpisodeInfo(title = title, episode = episodeNumber.toInt(), season =  seasonNumber.toInt(), episodeTitle =  episodeTitle, fullName =  cleanup(fullName))
    }

    private fun determineUndefinedFileName(): MediaInfo? {
        val serieEx = SerieEx(title, sanitizedName)
        val (season, episode) = serieEx.findSeasonAndEpisode(sanitizedName)
        val episodeNumber = serieEx.findEpisodeNumber()
        return if ((sanitizedName.contains(" - ") && episodeNumber != null) || season != null || episode != null) {
            determineSerieFileName()
        } else {
            determineMovieFileName()
        }
    }

    private fun cleanup(input: String): String {
        var cleaned = Regex("(?<=\\w)[_.](?=\\w)").replace(input, " ")
        cleaned = Regexes.illegalCharacters.replace(cleaned, " - ")
        cleaned = Regexes.trimWhiteSpaces.replace(cleaned, " ")
        return NameHelper.normalize(cleaned)
    }

    open internal class Base(val title: String, val sanitizedName: String) {
        fun getMatch(regex: String): String? {
            return Regex(regex, RegexOption.IGNORE_CASE).find(sanitizedName)?.value
        }

        fun removeResolutionAndBeyond(input: String): String? {
            val removalValue = Regex("(i?)([0-9].*[pk]|[ ._-]+[UHD]+[ ._-])").find(input)?.value ?: return null
            return input.substring(0, input.indexOf(removalValue))
        }

        fun yearRegex(): Regex {
            return Regex("[ .(][0-9]{4}[ .)]")
        }
    }

    internal class MovieEx(title: String, sanitizedName: String) : Base(title, sanitizedName) {
        /**
         * @return not null if matches " 2020 " or ".2020."
         */
        fun isDefinedWithYear(): Boolean {
            return getMatch(yearRegex().pattern)?.isNotBlank() ?: false
        }

        /**
         * Checks whether the filename contains the keyword movie, if so, default to movie
         */
        fun doesContainMovieKeywords(): Boolean {
            return getMatch("[(](?<=\\()movie(?=\\))[)]")?.isNotBlank() ?: false
        }
    }

    internal class SerieEx(title: String, sanitizedName: String) : Base(title, sanitizedName) {

        fun getSeasonEpisodeCombined(season: String, episode: String): String {
            return StringBuilder()
                .append("S")
                .append(if (season.length < 2) season.padStart(2, '0') else season)
                .append("E")
                .append(if (episode.length < 2) episode.padStart(2, '0') else episode)
                .toString().trim()
        }


        /**
         * Sjekken matcher tekst som dette:
         *      Cool - Season 1 Episode 13
         *      Cool - s1e13
         *      Cool - S1E13
         *      Cool - S1 13
         */
        fun findSeasonAndEpisode(inputText: String): Pair<String?, String?> {
            val matchResult = Regexes.SeasonEpisodeBlock.find(inputText)
            val season = matchResult?.groups?.get(1)?.value
            val episode = matchResult?.groups?.get(2)?.value
            return season to episode
        }

        fun findEpisodeNumber(): String? {
            val regex = Regex("\\b(\\d+)\\b")
            val matchResult = regex.findAll(sanitizedName)
            val usabeNumber = if (matchResult.toList().size > 1) {
                Regex("[-_] \\b(\\d+)\\b").find(sanitizedName)?.groups?.lastOrNull()?.value
            } else {
                matchResult.lastOrNull()?.value
            }
            return usabeNumber?.trim()
        }

        fun findEpisodeTitle(): String? {
            var startPosition: Int = 0
            startPosition = Regexes.SeasonEpisodeBlock.find(sanitizedName)?.value?.let { block ->
                sanitizedName.indexOf(block) + block.length
            } ?: 0

            val seCombo = findSeasonAndEpisode(sanitizedName)
            val episodeNumber = findEpisodeNumber()

            startPosition = if (startPosition != 0) startPosition else if (seCombo.second != null) sanitizedName.indexOf(seCombo.second!!)+ seCombo.second!!.length
            else if (episodeNumber != null) sanitizedName.indexOf(episodeNumber) + episodeNumber.length else 0
            val availableText = sanitizedName.substring(startPosition)

            val cleanedEpisodeTitle = availableText.replace(Regex("""(?i)\b(?:season|episode|ep)\b"""), "")
                .replace(Regex("""^\s*-\s*"""), "")
                .replace(Regex("""\s+"""), " ")
                .trim()

            return cleanedEpisodeTitle
        }
    }
}