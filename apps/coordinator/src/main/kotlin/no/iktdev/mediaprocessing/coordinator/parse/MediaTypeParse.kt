package no.iktdev.mediaprocessing.coordinator.parse

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.model.MediaType


fun IFile.evaluateMediaType(): MediaType {
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