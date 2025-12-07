package no.iktdev.mediaprocessing.coordinator.events

import no.iktdev.mediaprocessing.coordinator.listeners.events.MediaEventParsedInfoListener
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

class MediaEventParsedInfoListenerTest : MediaEventParsedInfoListener() {


    @MethodSource("fileNameSanitizeTest")
    @ParameterizedTest(name = "{0}")
    fun fileNameSanitizeTest(testCase: SanitizeTestCase) {
        val parser = FileNameParser(testCase.input)
        val result = parser.guessDesiredFileName()
        assertThat(result).isEqualTo(testCase.expected)
    }

    @MethodSource("parsedInfoTest")
    @ParameterizedTest(name = "{0}")
    fun parsedInfoTest(testCase: ParsedInfoTestCase) {
        val testFile = testCase.file
        val collection = testFile.getDesiredCollection()
        val fileName = testFile.guessDesiredFileName()
        val searchTitles = testFile.guessSearchableTitle()
        assertThat(collection).isEqualTo(testCase.expectedTitle)
        assertThat(fileName).isEqualTo(testCase.expectedFileName)
        assertThat(searchTitles).isEqualTo(testCase.expectedSearchTitles)
    }

    @MethodSource("parseVideoType")
    @ParameterizedTest(name = "{0}")
    fun parseVideoType(testCase: ParseVideoTypeTestCase) {
        val testFile = testCase.file
        val mediaType = testFile.guessMovieOrSeries()
        assertThat(mediaType).isEqualTo(testCase.expectedType)
    }


    data class SanitizeTestCase(
        val input: String,
        val expected: String
    )

    data class ParsedInfoTestCase(
        val file: File,
        val expectedTitle: String,
        val expectedFileName: String,
        val expectedSearchTitles: List<String>
    )

    data class ParseVideoTypeTestCase(
        val file: File,
        val expectedType: MediaType
    )

    companion object {
        @JvmStatic
        fun fileNameSanitizeTest() = listOf(
            Named.of(
                "Basic sanitization",
                SanitizeTestCase(
                    input = "Fancy.Thomas.S03E03.Enemy.1080p.AMAZING.WEB-VALUE.DDP5AN.1.H.264",
                    expected = "Fancy Thomas S03E03 Enemy"
                )
            ),
            Named.of(
                "Name with numbers",
                SanitizeTestCase(
                    input = "[TST] Fancy Name Test 99 - 01 [Nans][#00A8E6]",
                    expected = "Fancy Name Test 99 - 01"
                )
            ),
            Named.of(
                "Dot removal and special characters",
                SanitizeTestCase(
                    input = "Like.a.Potato.Chef.S01E01.Departure.\\u0026.Skills.1080p.Potato",
                    expected = "Like a Potato Chef S01E01 Departure \\u0026 Skills"
                )
            ),
            Named.of(
                "Movie name with numbers",
                SanitizeTestCase(
                    input = "Wicket.Wicker.Potato.4.2023.UHD.BluRay.2160p",
                    expected = "Wicket Wicker Potato 4"
                )
            ),
            Named.of(
                "Movie with extended title",
                SanitizeTestCase(
                    input = "Potato-Pass Movie - Skinke",
                    expected = "Potato-Pass Movie - Skinke"
                )
            ),
            Named.of(
                "Title with year in parentheses",
                SanitizeTestCase(
                    input = "Amazing Potato (2022) 1080p BluRay",
                    expected = "Amazing Potato"
                )
            ),
            Named.of(
                "Same",
                SanitizeTestCase(
                    input = "S01E03-How to unlucky i am",
                    expected = "S01E03-How to unlucky i am"
                )
            )
        )

        @JvmStatic
        fun parsedInfoTest() = listOf(
            Named.of(
                "Series episode parsing",
                ParsedInfoTestCase(
                    file = File("Fancy.Thomas.S03E03.Enemy.1080p.AMAZING.WEB-VALUE.DDP5AN.1.H.264.mkv"),
                    expectedTitle = "Fancy Thomas",
                    expectedFileName = "Fancy Thomas - S03E03 - Enemy",
                    expectedSearchTitles = listOf("Fancy Thomas", "Fancy Thomas - S03E03 - Enemy")
                )
            ),
            Named.of(
                "Movie parsing with year",
                ParsedInfoTestCase(
                    file = File("Epic.Potato.Movie.2021.1080p.BluRay.x264.mkv"),
                    expectedTitle = "Epic Potato Movie",
                    expectedFileName = "Epic Potato Movie",
                    expectedSearchTitles = listOf("Epic Potato Movie")
                )
            ),
            Named.of(
                "Series with dots and special characters",
                ParsedInfoTestCase(
                    file = File("Like.a.Potato.Chef.S01E01.Departure.\\u0026.Skills.1080p.Potato.mkv"),
                    expectedTitle = "Like a Potato Chef",
                    expectedFileName = "Like a Potato Chef - S01E01 - Departure \\u0026 Skills",
                    expectedSearchTitles = listOf("Like a Potato Chef", "Like a Potato Chef - S01E01 - Departure \\u0026 Skills")
                )
            ),
            Named.of(
                "Movie with extended title",
                ParsedInfoTestCase(
                    file = File("Potato-Pass Movie - Skinke.mkv"),
                    expectedTitle = "Potato-Pass Movie",
                    expectedFileName = "Potato-Pass Movie - Skinke",
                    expectedSearchTitles = listOf("Potato-Pass Movie", "Potato-Pass Movie - Skinke")
                )
            )
        )

        @JvmStatic
        fun parseVideoType() = listOf(
            Named.of(
                "Series file detection full block",
                ParseVideoTypeTestCase(
                    file = File("Fancy.Thomas.S03E03.Enemy.1080p.AMAZING.WEB-VALUE.DDP5AN.1.H.264.mkv"),
                    expectedType = MediaType.Serie
                )
            ),
            Named.of(
                "Series file detection fully spelt",
                ParseVideoTypeTestCase(
                    file = File("Potato harvesting Season 1 Episode 5 720p WEB-DL.mkv"),
                    expectedType = MediaType.Serie
                )
            ),
            Named.of(
                "Series file shorthand S full e",
                ParseVideoTypeTestCase(
                    file = File("Potato harvesting S1 - Episode 5 720p WEB-DL.mkv"),
                    expectedType = MediaType.Serie
                )
            ),
            Named.of(
                "Series file shorthand S and e",
                ParseVideoTypeTestCase(
                    file = File("Potato harvesting S1 - E5 720p WEB-DL.mkv"),
                    expectedType = MediaType.Serie
                )
            ),
            Named.of(
                "Movie file detection",
                ParseVideoTypeTestCase(
                    file = File("Epic.Potato.Movie.2021.1080p.BluRay.x264.mkv"),
                    expectedType = MediaType.Movie
                )
            ),
        )
    }


}