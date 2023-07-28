package no.iktdev.streamit.content.reader.analyzer.contentDeterminator

import no.iktdev.streamit.content.common.dto.reader.EpisodeInfo
import no.iktdev.streamit.content.common.dto.reader.MovieInfo
import no.iktdev.streamit.content.common.dto.reader.VideoInfo
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

data class DataHolder(
    val title: String,
    val sanitizedName: String,
    val ctype: FileNameDeterminate.ContentType = FileNameDeterminate.ContentType.UNDEFINED
)

class FileNameDeterminateTest {

    data class TestData(
        val expected: VideoInfo,
        val input: DataHolder
    )

    @ParameterizedTest
    @MethodSource("serieTestCases")
    fun testDetermineFileNameForSerie(namedTestData: TestData) {
        val fileNameDeterminate =
            FileNameDeterminate(
                namedTestData.input.title,
                namedTestData.input.sanitizedName,
                FileNameDeterminate.ContentType.SERIE
            )
        val result = fileNameDeterminate.getDeterminedVideoInfo()
        assertThat(result).isNotNull()
        assertThat(result?.fullName).isEqualTo(namedTestData.expected.fullName)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("movieTestCases")
    fun testDetermineFileNameForMovie(namedTestData: TestData) {
        val fileNameDeterminate =
            FileNameDeterminate(
                namedTestData.input.title,
                namedTestData.input.sanitizedName,
                FileNameDeterminate.ContentType.MOVIE
            )
        val result = fileNameDeterminate.getDeterminedVideoInfo()
        assertThat(result).isNotNull()
        assertThat(result?.fullName).isEqualTo(namedTestData.expected.fullName)
    }

    @ParameterizedTest()
    @MethodSource("undefinedTestCases")
    fun testDetermineFileNameForUndefined(namedTestData: TestData) {
        val fileNameDeterminate =
            FileNameDeterminate(
                namedTestData.input.title,
                namedTestData.input.sanitizedName,
                FileNameDeterminate.ContentType.UNDEFINED
            )
        val result = fileNameDeterminate.getDeterminedVideoInfo()
        assertThat(result).isNotNull()
        assertThat(result?.fullName).isEqualTo(namedTestData.expected.fullName)
    }

    @Test
    fun test() {
        val fileNameDeterminate = FileNameDeterminate(
            "Game of Thrones", "Game of Thrones - 01", FileNameDeterminate.ContentType.UNDEFINED
        )
        assertThat(fileNameDeterminate.getDeterminedVideoInfo()?.fullName).isEqualTo("Game of Thrones - S01E01")


        val td = TestData(
            expected = EpisodeInfo(title = "Game of Thrones", fullName = "Game of Thrones - S01E01", episode = 1, season = 1, episodeTitle = ""),
            input = DataHolder("Game of Thrones", "Game of Thrones - 01")
        )

        val fileNameDeterminate2 = FileNameDeterminate(
            td.input.title, td.input.sanitizedName, FileNameDeterminate.ContentType.UNDEFINED
        )
        assertThat(fileNameDeterminate2.getDeterminedVideoInfo()?.fullName).isEqualTo(td.expected.fullName)

    }

    @Test
    fun testWildStuff() {
        val fileNameDeterminate = FileNameDeterminate(
            "The Potato man", "The.Potato.man.2023.1080p.L950XL.x265-WIN10", FileNameDeterminate.ContentType.UNDEFINED
        )
        assertThat(fileNameDeterminate.getDeterminedVideoInfo()?.fullName).isEqualTo("The Potato man")
    }

    companion object {
        @JvmStatic
        fun serieTestCases(): List<Named<TestData>> {
            return listOf(
                Named.of("Is defined", TestData(
                    expected = EpisodeInfo(title = "Iseleve", fullName = "Iseleve - S01E13", episode = 13, season = 1, episodeTitle = "" ),
                    DataHolder("Iseleve","Iseleve - 13")
                )),
                Named.of("Is defined", TestData(
                    expected = EpisodeInfo(title = "Iseleve", fullName = "Iseleve - S01E13 - potetmos", episode = 13, season = 1, episodeTitle = "potetmos" ),
                    input = DataHolder("Iseleve","Iseleve - 13 potetmos")
                )),
                Named.of("Season and Episode in S01E01 format", TestData(
                    expected = EpisodeInfo(title = "Iseleve", fullName = "Iseleve - S01E13", episode = 13, season = 1, episodeTitle = "" ),
                    input = DataHolder("Iseleve","Iseleve - S1E13")
                )),


                Named.of("Season and Episode with episode title", TestData(
                    expected = EpisodeInfo(title = "Iseleve", fullName = "Iseleve - S01E13 - potetmos", episode = 13, season = 1, episodeTitle = "potetmos" ),
                    input = DataHolder("Iseleve","Iseleve - S1E13 potetmos")
                )),
                Named.of("Season and Episode with space separator", TestData(
                    expected = EpisodeInfo(title = "Iseleve", fullName = "Iseleve - S01E13", episode = 13, season = 1, episodeTitle = "" ),
                    input = DataHolder("Iseleve","Iseleve - S1 13")
                )),
                Named.of("Season and Episode with space separator and episode title", TestData(
                    expected = EpisodeInfo(title = "Iseleve", fullName = "Iseleve - S01E13 - potetos", episode = 13, season = 1, episodeTitle = "" ),
                    input = DataHolder("Iseleve","Iseleve - S1 13 potetos")
                )),
                Named.of("Lowercase season and episode", TestData(
                    expected = EpisodeInfo(title = "Iseleve", fullName = "Iseleve - S01E13", episode = 13, season = 1, episodeTitle = "" ),
                    input = DataHolder("Iseleve","Iseleve - s1e13")
                )),
                Named.of("Episode title with Season and Episode in text", TestData(
                    expected = EpisodeInfo(title = "Iseleve", fullName = "Iseleve - S01E13", episode = 13, season = 1, episodeTitle = "" ),
                    input = DataHolder("Iseleve","Iseleve - Season 1 Episode 13")
                )),
                Named.of("Episode title with Season and Episode in text and episode title", TestData(
                    expected = EpisodeInfo(title = "Iseleve", fullName = "Iseleve - S01E13 - Potetmos", episode = 13, season = 1, episodeTitle = "Potetmos" ),
                    input = DataHolder("Iseleve","Iseleve - Season 1 Episode 13 Potetmos")
                )),
            )
        }

        @JvmStatic
        fun movieTestCases(): List<Named<TestData>> {
            return listOf(
                Named.of(
                    "Movie with year", TestData(
                        MovieInfo("Some Movie", "Some Movie"),
                        DataHolder("Some Movie (2012)", "Some Movie (2012)", FileNameDeterminate.ContentType.MOVIE)
                    )
                ),
                Named.of(
                    "Movie without year", TestData(
                        MovieInfo("Another Movie", "Another Movie"),
                        DataHolder("Another Movie", "Another Movie", FileNameDeterminate.ContentType.MOVIE)
                    )

                ),
                Named.of(
                    "Movie with year and additional info", TestData(
                        expected = MovieInfo("Awesome Movie", "Awesome Movie - Part 1"),
                        DataHolder("Awesome Movie (2012) - Part 1", "Awesome Movie (2012) - Part 1")
                    )

                ),
                Named.of("Movie with title as '2012'", TestData(
                    expected = MovieInfo("2012", "2012"),
                    DataHolder("2012", "2012")
                )),
                Named.of("Movie with title as '2020'", TestData(
                    expected = MovieInfo("2020", "2020"),
                    DataHolder("2020 (2012)", "2020 (2012)")
                )),
                Named.of("Movie with title as '2049'", TestData(
                    expected = MovieInfo("2049", "2049"),
                    DataHolder("2049 (2017)", "2049 (2017)")
                )),
                Named.of("Movie with title as '3000'", TestData(
                    expected = MovieInfo("3000", "3000"),
                    DataHolder("3000 (2000)", "3000 (2000)")
                )),
                Named.of("Avengers - Endgame", TestData(
                    expected = MovieInfo("Avengers", "Avengers - Endgame"),

                    DataHolder("Avengers - Endgame", "Avengers - Endgame")
                )),
                Named.of(
                    "Ghost in the Shell (S.A.C) - Solid State Society", TestData(
                        expected = MovieInfo("Ghost in the Shell", "Ghost in the Shell (S A C) - Solid State Society"),
                        DataHolder(
                            "Ghost in the Shell - Solid State Society",
                            "Ghost in the Shell (S.A.C) - Solid State Society"
                        )
                    )
                ),
            )
        }

        @JvmStatic
        fun undefinedTestCases(): List<Named<TestData>> {
            return listOf(
                Named.of("Undefined - Movie", TestData(
                    expected = MovieInfo("Avengers", "Avengers - Endgame"),
                    input = DataHolder("Avengers", "Avengers - Endgame")
                )),
                Named.of("Undefined - Series", TestData(
                    expected = MovieInfo("Stranger Things", "Stranger Things"),
                    input = DataHolder("Stranger Things", "Stranger Things")
                )),
                Named.of("Undefined - Movie with Year", TestData(
                    expected = MovieInfo("Inception", "Inception"),
                    input = DataHolder("Inception", "Inception (2010)")
                )),
                Named.of("Undefined - Series with Year", TestData(
                    expected = MovieInfo("Friends", "Friends"),
                    input = DataHolder("Friends", "Friends (1994)")
                )),
                Named.of("Undefined - Movie with Genre", TestData(
                    expected = MovieInfo("The Dark Knight", "The Dark Knight"),
                    input = DataHolder("The Dark Knight", "The Dark Knight")
                )),
                Named.of("Undefined - Series with Genre", TestData(
                    expected = MovieInfo("Breaking Bad", "Breaking Bad"),
                    input = DataHolder("Breaking Bad", "Breaking Bad")
                )),
                Named.of(
                    "Undefined - Movie with Keywords",
                    TestData(
                        expected = MovieInfo("The Lord of the Rings", "The Lord of the Rings"),
                        input = DataHolder("The Lord of the Rings", "The Lord of the Rings (Movie)")
                    )
                ),
                Named.of("Undefined - Series with Keywords", TestData(
                    expected = EpisodeInfo("Game of Thrones", fullName = "Game of Thrones 01", episode = 1, season = 1, episodeTitle = ""),
                    input = DataHolder("Game of Thrones", "Game of Thrones 01")
                )),
                Named.of("Undefined - Series with Keywords", TestData(
                    expected = MovieInfo("Game of Thrones", fullName = "Game of Thrones 01"),
                    input = DataHolder("Game of Thrones", "Game of Thrones 01")
                )),
                Named.of(
                    "Undefined - Series with number",
                    TestData(
                        expected = EpisodeInfo(title = "Game of Thrones", fullName = "Game of Thrones - S01E01", episode = 1, season = 1, episodeTitle = ""),
                        input = DataHolder("Game of Thrones", "Game of Thrones - 01")
                    )
                ),
            )
        }
    }


}
