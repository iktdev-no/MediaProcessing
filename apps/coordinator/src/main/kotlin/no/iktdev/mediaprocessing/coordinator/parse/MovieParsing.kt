package no.iktdev.mediaprocessing.coordinator.parse

import no.iktdev.files.IFile

class MovieParsing : BaseParsing() {

    private val yearRegex = Regex("\\b(19|20)\\d{2}\\b")
    private val underscoreYearRegex = Regex(".*_((19|20)\\d{2})_.*")

    override fun extractCollection(file: IFile): String {
        val raw = file.nameWithoutExtension

        // Start med å rydde vekk brackets, parens, tags, oppløsning osv
        val cleaned = raw.cleanBasic()

        // Fjern årstall (de skal ikke være i collection)
        val noYear = yearRegex.replace(cleaned, " ").noExtraSpaces()

        // Ta ALT før første " - " som base‑tittel
        val base = noYear.split(" - ").first()

        return base.fullTrim()
    }


    override fun extractFilename(file: IFile): String {
        val raw = file.nameWithoutExtension
        val collection = extractCollection(file)

        // Vi trenger en "variant"-del (etter " - ") hvis den finnes
        val cleaned = raw.cleanBasic()
        val cleanedNoYear = yearRegex.replace(cleaned, " ").noExtraSpaces()
        val parts = cleanedNoYear.split(" - ", limit = 2)

        val variant = if (parts.size > 1) parts[1].fullTrim() else null

        // Spesialcase: my_movie_title_2019_1080p_x264_YTS
        // → collection = "my movie title"
        // → filename = "my movie title (2019)"
        val underscoreYearMatch = underscoreYearRegex.matchEntire(raw)
        val underscoreYear = underscoreYearMatch?.groupValues?.get(1)

        return when {
            variant != null && variant.isNotEmpty() -> "$collection - $variant"
            underscoreYear != null -> "$collection (${underscoreYear})"
            else -> collection
        }
    }

    override fun extractTitles(file: IFile): List<String> {
        val collection = extractCollection(file)
        val filename = extractFilename(file)

        val titles = mutableListOf<String>()

        val hasYearInFilename = yearRegex.containsMatchIn(filename)
        val hasYearInCollection = yearRegex.containsMatchIn(collection)

        // Hvis filnavnet har år og collection ikke har → år-varianten først
        if (hasYearInFilename && !hasYearInCollection) {
            titles.add(filename)
            if (filename != collection) titles.add(collection)
        } else {
            titles.add(collection)
            if (filename != collection) titles.add(filename)
        }

        return titles.distinct()
    }
}
