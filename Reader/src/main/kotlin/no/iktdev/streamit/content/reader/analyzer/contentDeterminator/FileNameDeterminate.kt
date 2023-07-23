package no.iktdev.streamit.content.reader.analyzer.contentDeterminator

import no.iktdev.streamit.content.common.dto.reader.EpisodeInfo
import no.iktdev.streamit.content.common.dto.reader.MovieInfo
import no.iktdev.streamit.content.common.dto.reader.VideoInfo

class FileNameDeterminate(val title: String, val sanitizedName: String, val ctype: ContentType = ContentType.UNDEFINED) {

    enum class ContentType {
        MOVIE,
        SERIE,
        UNDEFINED
    }

    fun getDeterminedFileName(): VideoInfo? {
        return when (ctype) {
            ContentType.MOVIE -> determineMovieFileName()
            ContentType.SERIE -> determineSerieFileName()
            ContentType.UNDEFINED -> determineUndefinedFileName()
        }
    }

    private fun determineMovieFileName(): MovieInfo? {
        val movieEx = MovieEx(title, sanitizedName)
        val result = when {
            movieEx.isDefinedWithYear() != null -> sanitizedName.replace(movieEx.isDefinedWithYear()!!, "").trim()
            movieEx.doesContainMovieKeywords() -> sanitizedName.replace(Regex("(?i)\\s*\\(\\s*movie\\s*\\)\\s*"), "").trim()
            else -> title
        }
        return MovieInfo(title, result)
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
        val fullName = "${useTitle.trim()} - $seasonEpisodeCombined ${if (episodeTitle.isNullOrEmpty()) "" else "- $episodeTitle"}".trim()
        return EpisodeInfo(title, episodeNumber.toInt(), seasonNumber.toInt(), episodeTitle, fullName)
    }

    private fun determineUndefinedFileName(): VideoInfo? {
        val serieEx = SerieEx(title, sanitizedName)
        val (season, episode) = serieEx.findSeasonAndEpisode(sanitizedName)
        return if (sanitizedName.contains(" - ") || season != null || episode != null) {
            determineSerieFileName()
        } else {
            determineMovieFileName()
        }
    }

    open internal class Base(val title: String, val sanitizedName: String) {
        fun getMatch(regex: String): String? {
            return Regex(regex, RegexOption.IGNORE_CASE).find(sanitizedName)?.value
        }
    }

    internal class MovieEx(title: String, sanitizedName: String) : Base(title, sanitizedName) {
        /**
         * @return not null if matches " 2020 " or ".2020."
         */
        fun isDefinedWithYear(): String? {
            return getMatch("[ .][0-9]{4}[ .]")
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
            val regex = Regex("""(?i)\b(?:S|Season)\s*(\d+).*?(?:E|Episode)?\s*(\d+)\b""")
            val matchResult = regex.find(inputText)
            val season = matchResult?.groups?.get(1)?.value
            val episode = matchResult?.groups?.get(2)?.value
            return season to episode
        }

        fun findEpisodeNumber(): String? {
            val regex = Regex("\\b(\\d+)\\b")
            val matchResult = regex.find(sanitizedName)
            return matchResult?.value?.trim()
        }

        fun findEpisodeTitle(): String? {
            val seCombo = findSeasonAndEpisode(sanitizedName)
            val episodeNumber = findEpisodeNumber()

            val startPosition = if (seCombo.second != null) sanitizedName.indexOf(seCombo.second!!)+ seCombo.second!!.length
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
