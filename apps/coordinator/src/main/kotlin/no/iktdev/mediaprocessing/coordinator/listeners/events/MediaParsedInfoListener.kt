package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.File

@Order(2)
@Component
class MediaParsedInfoListener : EventListener() {
    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val started = event as? StartProcessingEvent ?: return null
        val file = File(started.data.fileUri)

        val filename = file.guessDesiredFileName()
        val collection = file.getDesiredCollection()
        val searchTitles = file.guessSearchableTitle()
        val mediaType = file.guessMovieOrSeries()

        return MediaParsedInfoEvent(
            MediaParsedInfoEvent.ParsedData(
                parsedFileName = filename,
                parsedCollection = collection,
                parsedSearchTitles = searchTitles,
                mediaType = mediaType
            )
        ).derivedOf(event)
    }

    fun String.getCleanedTitle(): String {
        return this
            .noBrackets()
            .noParens()
            .noResolutionAndAfter()
            .noSourceTags()
            .noYear()
            .noDots()
            .noTrailingOrLeading()
            .noUnderscores()
            .noExtraSpaces()
            .trim()
    }


    fun String.noBrackets() = Regex("\\[.*?]").replace(this, " ")
    fun String.noParens() = Regex("\\(.*?\\)").replace(this, " ")
    fun String.noTrailingOrLeading() = Regex("^[^a-zA-Z0-9!,]+|[^a-zA-Z0-9!~,]+\$").replace(this, " ")
    fun String.noResolutionAndAfter() = Regex("[0-9]+[pk].*", RegexOption.IGNORE_CASE).replace(this, "")
    fun String.noSourceTags() =
        Regex("(?i)(bluray|laserdisc|dvd|web|uhd|hd|htds|imax).*", RegexOption.IGNORE_CASE).replace(this, " ")

    fun String.noUnderscores() = this.replace("_", " ")
    fun String.noYear() = Regex("\\b\\d{4}\\b").replace(this.takeIf { !it.matches(Regex("^\\d{4}")) } ?: this, "")
    fun String.noDots() = Regex("(?<!\\b(?:Dr|Mr|Ms|Mrs|Lt|Capt|Prof|St|Ave))\\.").replace(this, " ")
    fun String.noExtraSpaces() = Regex("\\s{2,}").replace(this, " ")
    fun String.fullTrim() = this.trim('.', ',', ' ', '_', '-')


    fun File.guessMovieOrSeries(): MediaType {
        val name = this.nameWithoutExtension.lowercase()

        // Serie-mønstre: dekker alle vanlige shorthand og varianter
        val seriesPatterns = listOf(
            Regex("s\\d{1,2}e\\d{1,2}"),              // S01E03, s1e5
            Regex("\\d{1,2}x\\d{1,2}"),               // 1x03, 2x10
            Regex("season\\s*\\d+"),                  // Season 2
            Regex("episode\\s*\\d+"),                 // Episode 5
            Regex("ep\\s*\\d+"),                      // Ep05, Ep 5
            Regex("s\\d{1,2}\\s*[- ]\\s*e\\d{1,2}"),  // S1 - E5, S01 - E05
            Regex("s\\d{1,2}\\s*ep\\s*\\d{1,2}"),     // S1 Ep05
            Regex("series\\s*\\d+"),                  // Series 2 (britisk stil)
            Regex("s\\d{1,2}[. ]e\\d{1,2}")           // S01.E02 eller S01 E02
        )

        if (seriesPatterns.any { it.containsMatchIn(name) }) {
            return MediaType.Serie
        }

        // Film-mønstre: årstall (1900–2099) etter tittel
        val moviePattern = Regex("\\b(19|20)\\d{2}\\b")
        if (moviePattern.containsMatchIn(name)) {
            return MediaType.Movie
        }

        // Fallback: hvis ingen mønstre passer, anta film
        return MediaType.Movie
    }


    fun File.guessDesiredFileName(): String {
        val type = this.guessMovieOrSeries()
        return when (type) {
            MediaType.Movie -> this.guessDesiredMovieTitle()
            MediaType.Serie -> this.guessDesiredSerieTitle()
        }
    }

    fun File.getDesiredCollection(): String {
        val collection = when (this.guessMovieOrSeries()) {
            MediaType.Movie -> this.guessDesiredMovieTitle()
            MediaType.Serie -> this.guessDesiredSerieTitle()
        }
        return collection.noParens().noYear().split(" - ").first().trim()
    }

    /**
     * @return A fully cleaned title suitable to use for collection
     */
    fun File.guessDesiredMovieTitle(): String {
        val cleaned = this.nameWithoutExtension.getCleanedTitle()
        val yearRegex = Regex("\\b(19|20)\\d{2}\\b")
        val yearMatch = yearRegex.find(cleaned)

        return if (yearMatch != null) {
            val title = cleaned.replace(yearRegex, "").trim()
            "${title} (${yearMatch.value})"
        } else {
            cleaned
        }
    }


    /**
     * @return A fully cleaned title including season and episode with possible episode title
     */
    fun File.guessDesiredSerieTitle(): String {
        val raw = this.nameWithoutExtension

        val seasonRegex = Regex("""(?i)(?:S|Season|Series)\s*(\d{1,2})""")
        val episodeRegex = Regex("""(?i)(?:E|Episode|Ep)\s*(\d{1,3})""")
        val revisionRegex = Regex("""(?i)\bv(\d+)\b""")
        val seasonEpisodeRegex = Regex("""(?i)(\d{1,2})x(\d{1,2})(?:[vV](\d+))?""")

        var season: Int? = null
        var episode: Int? = null
        var revision: Int? = null
        var baseTitle = raw.getCleanedTitle()
        var episodeTitle = ""

        val seMatch = seasonEpisodeRegex.find(raw)
        if (seMatch != null) {
            season = seMatch.groupValues[1].toIntOrNull()
            episode = seMatch.groupValues[2].toIntOrNull()
            revision = seMatch.groupValues.getOrNull(3)?.toIntOrNull()
            baseTitle = raw.substring(0, seMatch.range.first).getCleanedTitle()
            episodeTitle = raw.substring(seMatch.range.last + 1).getCleanedTitle()
        } else {
            val seasonMatch = seasonRegex.find(raw)
            val episodeMatch = episodeRegex.find(raw)
            val revisionMatch = revisionRegex.find(raw)

            season = seasonMatch?.groupValues?.get(1)?.toIntOrNull()
            episode = episodeMatch?.groupValues?.get(1)?.toIntOrNull()
            revision = revisionMatch?.groupValues?.get(1)?.toIntOrNull()

            baseTitle = if (seasonMatch != null) {
                raw.substring(0, seasonMatch.range.first).getCleanedTitle()
            } else raw.getCleanedTitle()

            episodeTitle = if (episodeMatch != null) {
                raw.substring(episodeMatch.range.last + 1).getCleanedTitle()
            } else ""
        }

        // Fallback: hvis baseTitle er tom eller bare inneholder S/E, bruk parent‑mappe
        if (baseTitle.isBlank() || baseTitle.matches(Regex("""(?i)^s?\d+e?\d+$"""))) {
            baseTitle = this.parentFile?.name?.getCleanedTitle() ?: "Dumb ways to die"
        }

        val tag = buildString {
            append("S${(season ?: 1).toString().padStart(2, '0')}")
            append("E${(episode ?: 1).toString().padStart(2, '0')}")
            if (revision != null) append(" (v$revision)")
        }

        return buildString {
            append(baseTitle)
            append(" - ")
            append(tag)
            if (episodeTitle.isNotEmpty()) {
                append(" - ")
                append(episodeTitle)
            }
        }.trim()
    }



    fun File.guessSearchableTitle(): List<String> {
        val cleaned = this.guessDesiredFileName()
            .noResolutionAndAfter()
            .noSourceTags()
            .noDots()
            .noExtraSpaces()
            .fullTrim()

        val titles = mutableListOf<String>()

        val yearRegex = Regex("""\b(19|20)\d{2}\b""")
        val hasYear = yearRegex.containsMatchIn(cleaned)

        // 1. Hvis årstall finnes, legg hele cleaned først
        if (hasYear) {
            titles.add(cleaned)
        }

        // 2. Første del før bindestrek
        val firstPart = cleaned.split(" - ").firstOrNull()?.trim() ?: cleaned
        titles.add(firstPart)

        // 3. Hele cleaned (hvis ikke allerede lagt inn først)
        if (!hasYear) {
            titles.add(cleaned)
        }

        // 4. Variant uten årstall
        val noYear = yearRegex.replace(cleaned, "")
            .noParens().trim()
        if (noYear.isNotEmpty() && noYear != cleaned) {
            titles.add(noYear)
        }

        return titles.distinct()
    }


}