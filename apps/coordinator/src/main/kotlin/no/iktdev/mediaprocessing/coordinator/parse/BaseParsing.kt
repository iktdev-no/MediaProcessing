package no.iktdev.mediaprocessing.coordinator.parse

import no.iktdev.mediaprocessing.coordinator.listeners.events.MediaParsedInfoListener
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import java.io.File

abstract class BaseParsing {
    fun String.noResolutionAndAfter() = Regex("[0-9]+[pk].*", RegexOption.IGNORE_CASE).replace(this, "")
    fun String.noSourceTags() =
        Regex("(?i)(bluray|laserdisc|dvd|web|uhd|hd|htds|imax).*", RegexOption.IGNORE_CASE).replace(this, " ")
    fun String.noDots() = Regex("(?<!\\b(?:Dr|Mr|Ms|Mrs|Lt|Capt|Prof|St|Ave))\\.").replace(this, " ")
    fun String.noExtraSpaces() = Regex("\\s{2,}").replace(this, " ")
    fun String.fullTrim(): String {
        // Fjern støy i starten
        val leadingTrimmed = this.replace(Regex("^[^\\p{L}\\p{N}]+"), "")
        // Fjern støy på slutten
        return leadingTrimmed.replace(Regex("[^\\p{L}\\p{N}]+$"), "").trim()
    }


    fun String.noParens() = Regex("\\(.*?\\)").replace(this, " ")


    fun String.cleanBasic(): String {
        return this
            // Fjern [brackets]
            .replace(Regex("\\[.*?]"), " ")
        // Fjern (parenteser)
        .replace(Regex("\\(.*?\\)"), " ")
            // Fjern oppløsning og alt etter (1080p, 4k, 720p, etc.)
            .replace(Regex("[0-9]+[pk].*", RegexOption.IGNORE_CASE), " ")
            // Fjern source tags (BluRay, WEB, HDRip, etc.)
            .replace(Regex("(?i)(bluray|laserdisc|dvd|web|uhd|hd|imax).*"), " ")
            // Underscores → space
            .replace("_", " ")
            // Fjern punktum som ikke er del av forkortelser
            .replace(Regex("(?<!\\b(?:Dr|Mr|Ms|Mrs|Lt|Capt|Prof|St|Ave))\\."), " ")
            // Fjern dobbel whitespace
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    abstract fun extractCollection(file: File): String
    abstract fun extractTitles(file: File): List<String>
    abstract fun extractFilename(file: File): String
    open fun extractEpisodeInfo(file: File): MediaParsedInfoEvent.ParsedData.EpisodeInfo? = null
}