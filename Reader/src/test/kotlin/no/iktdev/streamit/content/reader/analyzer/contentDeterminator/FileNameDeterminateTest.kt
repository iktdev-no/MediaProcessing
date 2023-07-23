package no.iktdev.streamit.content.reader.analyzer.contentDeterminator

import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource


class FileNameDeterminateTest {

    data class TestData(
        val expected: String,
        val input: String
    )

    @ParameterizedTest
    @MethodSource("serieTestCases")
    fun testDetermineFileNameForSerie(namedTestData: TestData) {
        val fileNameDeterminate =
            FileNameDeterminate("Iseleve", namedTestData.input, FileNameDeterminate.ContentType.SERIE)
        assertEquals(
            namedTestData.expected,
            fileNameDeterminate.getDeterminedVideoInfo()?.fullName,
            "Test case: ${namedTestData.input}"
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("movieTestCases")
    fun testDetermineFileNameForMovie(namedTestData: TestData) {
        val fileNameDeterminate = FileNameDeterminate(
            namedTestData.input, namedTestData.input, FileNameDeterminate.ContentType.MOVIE
        )
        assertEquals(
            namedTestData.expected,
            fileNameDeterminate.getDeterminedVideoInfo()?.fullName,
            "Test case: ${namedTestData.input}"
        )
    }

    @ParameterizedTest()
    @MethodSource("undefinedTestCases")
    fun testDetermineFileNameForUndefined(namedTestData: TestData) {
        val fileNameDeterminate = FileNameDeterminate(
            namedTestData.input, namedTestData.input, FileNameDeterminate.ContentType.UNDEFINED
        )
        assertThat(fileNameDeterminate.getDeterminedVideoInfo()?.fullName).isEqualTo(namedTestData.expected)
    }

    @Test
    fun test() {
        val namedTestData = TestData("Game of Thrones - S01E01", "Game of Thrones - 01")
        val fileNameDeterminate = FileNameDeterminate(
            namedTestData.input, namedTestData.input, FileNameDeterminate.ContentType.UNDEFINED
        )
        assertThat(fileNameDeterminate.getDeterminedVideoInfo()?.fullName).isEqualTo(namedTestData.expected)
    }

    companion object {
        @JvmStatic
        fun serieTestCases(): List<Named<TestData>> {
            return listOf(
                Named.of("Is defined", TestData("Iseleve - S01E13", "Iseleve - 13")),
                Named.of("Contains episode title", TestData("Iseleve - S01E13 - potetmos", "Iseleve - 13 potetmos")),
                Named.of("Season and Episode in S01E01 format", TestData("Iseleve - S01E13", "Iseleve - S1E13")),
                Named.of(
                    "Season and Episode with episode title",
                    TestData("Iseleve - S01E13 - potetmos", "Iseleve - S1E13 potetmos")
                ),
                Named.of("Season and Episode with space separator", TestData("Iseleve - S01E13", "Iseleve - S1 13")),
                Named.of(
                    "Season and Episode with space separator and episode title",
                    TestData("Iseleve - S01E13 - potetos", "Iseleve - S1 13 potetos")
                ),
                Named.of("Lowercase season and episode", TestData("Iseleve - S01E13", "Iseleve - s1e13")),
                Named.of(
                    "Episode title with Season and Episode in text",
                    TestData("Iseleve - S01E13", "Iseleve - Season 1 Episode 13")
                ),
                Named.of(
                    "Episode title with Season and Episode in text and episode title",
                    TestData("Iseleve - S01E13 - Potetmos", "Iseleve - Season 1 Episode 13 Potetmos")
                )
            )
        }

        @JvmStatic
        fun movieTestCases(): List<Named<TestData>> {
            return listOf(
                Named.of("Movie with year", TestData("Some Movie (2012)", "Some Movie (2012)")),
                Named.of("Movie without year", TestData("Another Movie", "Another Movie")),
                Named.of("Movie with year and additional info", TestData("Awesome Movie (2012) - Part 1", "Awesome Movie (2012) - Part 1")),
                //Named.of("Movie with year and spaces", TestData("Space Odyssey (2010)", "Space Odyssey      (2010)")),
                //Named.of("Movie with year and parentheses", TestData("Sci-Fi Movie (2015)", "Sci-Fi Movie (((2015)))")),
                //Named.of("Movie with year and hyphen", TestData("Action Flick (2008)", "Action Flick - 2008")),
                //Named.of("Movie with year and brackets", TestData("Blockbuster (2011)", "Blockbuster [2011]")),
                //Named.of("Movie with year and period", TestData("Time Travelers. (2022)", "Time Travelers. .2022.")),
                //Named.of("Movie with year and underscores", TestData("Hidden Gem (1999)", "Hidden Gem _1999_")),
                Named.of("Movie with title as '2012'", TestData("2012", "2012")),
                Named.of("Movie with title as '2020'", TestData("2020 (2012)", "2020 (2012)")),
                Named.of("Movie with title as '2049'", TestData("2049 (2017)", "2049 (2017)")),
                Named.of("Movie with title as '3000'", TestData("3000 (2000)", "3000 (2000)"))
            )
        }

        @JvmStatic
        fun undefinedTestCases(): List<Named<TestData>> {
            return listOf(
                Named.of("Undefined - Movie", TestData("Avengers - Endgame", "Avengers - Endgame")),
                Named.of("Undefined - Series", TestData("Stranger Things", "Stranger Things")),
                Named.of("Undefined - Movie with Year", TestData("Inception (2010)", "Inception (2010)")),
                Named.of("Undefined - Series with Year", TestData("Friends (1994)", "Friends (1994)")),
                Named.of("Undefined - Movie with Genre", TestData("The Dark Knight", "The Dark Knight")),
                Named.of("Undefined - Series with Genre", TestData("Breaking Bad", "Breaking Bad")),
                Named.of("Undefined - Movie with Keywords", TestData("The Lord of the Rings", "The Lord of the Rings (Movie)")),
                Named.of("Undefined - Series with Keywords", TestData("Game of Thrones 01", "Game of Thrones 01")),
                Named.of("Undefined - Series with number", TestData("Game of Thrones - S01E01", "Game of Thrones - 01")),
            )
        }
    }


}
