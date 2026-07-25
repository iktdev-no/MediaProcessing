package no.iktdev.mediaprocessing.coordinator.parse

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent

class SerieParsing : BaseParsing() {

    // Scene format: 1x02 / 02x05 / 1x02v2
    private val seasonEpisodeRegex = Regex("""(?i)(\d{1,2})x(\d{1,2})(?:[vV](\d+))?""")

    // Standard format: S01E02
    private val seasonRegex = Regex("""(?i)(?:S|Season|Series)\s*(\d{1,2})""")
    private val episodeRegex = Regex("""(?i)(?:E|Episode|Ep)\s*(\d{1,3})""")

    // Sonarr-style anime detection
    // Eksempler:
    // "Show - 01"
    // "Show - 01v2"
    // "Show - 01 [1080p]"
    private val animeEpisodeRegex = Regex("""(?<!\d)(\d{1,3})(?:v\d+)?(?=[\s\]\-]|$)""", RegexOption.IGNORE_CASE)

    override fun extractEpisodeInfo(file: IFile): MediaParsedInfoEvent.ParsedData.EpisodeInfo {
        val raw = file.nameWithoutExtension.cleanNoise()

        // -----------------------------------------------------
        // Case 1: 1x02
        // -----------------------------------------------------
        val seMatch = seasonEpisodeRegex.find(raw)

        if (seMatch != null) {

            val season = seMatch.groupValues[1].toInt()
            val episode = seMatch.groupValues[2].toInt()

            // Revision (v2/v3) ignoreres
            @Suppress("UNUSED_VARIABLE")
            val revision = seMatch.groupValues.getOrNull(3)?.toIntOrNull()

            val episodeTitle = raw.substring(seMatch.range.last + 1)
                .removePrefix("-")
                .removePrefix(".")
                .removePrefix("_")
                .cleanBasic()
                .fullTrim()

            return MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                episodeNumber = episode,
                seasonNumber = season,
                episodeTitle = episodeTitle
            )
        }

        // -----------------------------------------------------
        // Case 2: S01E02
        // -----------------------------------------------------
        val season = seasonRegex.find(raw)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val epMatch = episodeRegex.find(raw)

        if (epMatch != null) {

            val episode = epMatch.groupValues[1].toInt()

            val episodeTitle = raw.substring(epMatch.range.last + 1)
                .removePrefix("-")
                .removePrefix(".")
                .removePrefix("_")
                .cleanBasic()
                .fullTrim()

            return MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                episodeNumber = episode,
                seasonNumber = season,
                episodeTitle = episodeTitle
            )
        }

        // -----------------------------------------------------
        // Case 3: Anime numbering
        // -----------------------------------------------------
        val animeMatch = animeEpisodeRegex.find(raw)

        if (animeMatch != null) {

            val episode = animeMatch.groupValues[1].toInt()

            val episodeTitle = raw.substring(animeMatch.range.last + 1)
                .removePrefix("-")
                .removePrefix(".")
                .removePrefix("_")
                .cleanBasic()
                .fullTrim()

            return MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                episodeNumber = episode,
                seasonNumber = 1,
                episodeTitle = episodeTitle
            )
        }

        // -----------------------------------------------------
        // Fallback
        // -----------------------------------------------------
        return MediaParsedInfoEvent.ParsedData.EpisodeInfo(
            episodeNumber = 1,
            seasonNumber = 1,
            episodeTitle = ""
        )
    }

    override fun extractCollection(file: IFile): String {
        return sharedCollectionExtractor(file).ifBlank { extractCollectionFromContainingFolder(file) }
    }

    fun sharedCollectionExtractor(file: IFile): String {
        val raw = file.nameWithoutExtension.cleanNoise()

        val seMatch = seasonEpisodeRegex.find(raw)
        val seasonMatch = seasonRegex.find(raw)
        val animeMatch = animeEpisodeRegex.find(raw)

        val cutIndex = when {
            seMatch != null -> seMatch.range.first
            seasonMatch != null -> seasonMatch.range.first
            animeMatch != null -> animeMatch.range.first
            else -> raw.length
        }

        val beforeSeason = raw.substring(0, cutIndex)

        val base = beforeSeason.split(" - ").first()

        val noYearInParens = base.replace(Regex("\\(\\s*(19|20)\\d{2}\\s*\\)"), " ")

        val cleaned = noYearInParens.cleanBasic()

        return cleaned.fullTrim()
    }

    fun extractCollectionFromContainingFolder(file: IFile): String {
        return sharedCollectionExtractor(file.parentFile)
    }

    override fun extractFilename(file: IFile): String {

        val collection = extractCollection(file)
        val ep = extractEpisodeInfo(file)

        val tag = buildString {
            append("S${ep.seasonNumber.toString().padStart(2, '0')}")
            append("E${ep.episodeNumber.toString().padStart(2, '0')}")
        }

        return buildString {
            append(collection)
            append(" - ")
            append(tag)

            if (ep.episodeTitle?.isNotBlank() == true) {
                append(" - ")
                append(ep.episodeTitle)
            }

        }.trim()
    }

    override fun extractTitles(file: IFile): List<String> {

        val collection = extractCollection(file)

        val numberInTitle = Regex("\\b\\d{2,4}\\b").containsMatchIn(collection)

        return when {
            numberInTitle -> listOf(collection)
            else -> listOf(collection)
        }
    }

    private fun String.cleanNoise(): String {
        return this.replace(Regex("""\[[^\]]*]"""), " ") // Fjerner alt i ecklammer [...]
            .replace(Regex("""\([^)]*\)"""), " ") // Fjerner alt i parenteser (...)
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}