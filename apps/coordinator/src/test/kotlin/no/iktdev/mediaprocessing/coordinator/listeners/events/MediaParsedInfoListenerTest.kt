package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.coordinator.parse.evaluateMediaType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaParsedInfoListenerTest : TestBase() {
    val listener = MediaParsedInfoListener();

    @ParameterizedTest(name = "{0}")
    @MethodSource("parsedInfoTestCases")
    fun parsedInfoTest(testCase: ParsedInfoTestCase) {
        val startEvent = StartProcessingEvent(data = StartData(
            operation = emptySet(),
            flow = StartFlow.Auto,
            fileUri = testCase.file.absolutePath
        )).newReferenceId()

        val result = listener.onEvent(startEvent, history) as? MediaParsedInfoEvent
        assertThat(result).isNotNull
        assertThat(result!!.data).isNotNull
        Assertions.assertThat(result.data.parsedCollection).isEqualTo(testCase.expectedCollection)
        Assertions.assertThat(result.data.parsedFileName).isEqualTo(testCase.expectedFileName)
        Assertions.assertThat(result.data.parsedSearchTitles).isEqualTo(testCase.expectedSearchTitles)
    }

    @MethodSource("parseVideoTypeCases")
    @ParameterizedTest(name = "{0}")
    fun parseVideoType(testCase: ParseVideoTypeTestCase) {
        val testFile = testCase.file
        val mediaType = testFile.evaluateMediaType()
        Assertions.assertThat(mediaType).isEqualTo(testCase.expectedType)
    }

    data class ParsedInfoTestCase(
        val file: File,
        val expectedCollection: String,
        val expectedFileName: String,
        val expectedSearchTitles: List<String>,
        val expectedEpisodeInfo: MediaParsedInfoEvent.ParsedData.EpisodeInfo? = null
    )

    data class ParseVideoTypeTestCase(
        val file: File,
        val expectedType: MediaType
    )

    companion object {

        @JvmStatic
        fun parsedInfoTestCases() = listOf(
            // existing parsed cases
            Named.of(
                "Serie with year in the future",
                ParsedInfoTestCase(
                    file = File("Demon Lord 2099 - S01E01 - Cyberpunk City Shinjuku.mkv"),
                    expectedCollection = "Demon Lord 2099",
                    expectedFileName = "Demon Lord 2099 - S01E01 - Cyberpunk City Shinjuku",
                    expectedSearchTitles = listOf(
                        "Demon Lord 2099"
                    ),
                    expectedEpisodeInfo = MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                        seasonNumber = 1,
                        episodeNumber = 1,
                        episodeTitle = "Cyberpunk City Shinjuku"
                    )
                ),
            ),
            Named.of(
                "Series episode parsing",
                ParsedInfoTestCase(
                    file = File("Fancy.Thomas.S03E03.Enemy.1080p.AMAZING.WEB-VALUE.DDP5AN.1.H.264.mkv"),
                    expectedCollection = "Fancy Thomas",
                    expectedFileName = "Fancy Thomas - S03E03 - Enemy",
                    expectedSearchTitles = listOf(
                        "Fancy Thomas",),
                    expectedEpisodeInfo = MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                        seasonNumber = 3,
                        episodeNumber = 3,
                        episodeTitle = "Enemy"
                    )
                )
            ),
            Named.of(
                "Movie parsing with year",
                ParsedInfoTestCase(
                    file = File("Epic.Potato.Movie.2021.1080p.BluRay.x264.mkv"),
                    expectedCollection = "Epic Potato Movie",
                    expectedFileName = "Epic Potato Movie",
                    expectedSearchTitles = listOf("Epic Potato Movie"),
                    expectedEpisodeInfo = null
                )
            ),
            Named.of(
                "Series with dots and special characters",
                ParsedInfoTestCase(
                    file = File("Like.a.Potato.Chef.S01E01.Departure.\\u0026.Skills.1080p.Potato.mkv"),
                    expectedCollection = "Like a Potato Chef",
                    expectedFileName = "Like a Potato Chef - S01E01 - Departure \\u0026 Skills",
                    expectedSearchTitles = listOf(
                        "Like a Potato Chef",
                    ),
                    expectedEpisodeInfo = MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                        seasonNumber = 1,
                        episodeNumber = 1,
                        episodeTitle = "Departure \\u0026 Skills"
                    )
                )
            ),
            Named.of(
                "Movie with extended title",
                ParsedInfoTestCase(
                    file = File("Potato-Pass Movie - Skinke.mkv"),
                    expectedCollection = "Potato-Pass Movie",
                    expectedFileName = "Potato-Pass Movie - Skinke",
                    expectedSearchTitles = listOf("Potato-Pass Movie", "Potato-Pass Movie - Skinke")
                )
            ),

            Named.of(
                "Name with numbers",
                ParsedInfoTestCase(
                    file = File("[TST] Fancy Name Test 99 - 01 [Nans][#00A8E6].mkv"),
                    expectedCollection = "Fancy Name Test 99",
                    expectedFileName = "Fancy Name Test 99 - 01",
                    expectedSearchTitles = listOf("Fancy Name Test 99", "Fancy Name Test 99 - 01")
                )
            ),
            Named.of(
                "Movie name with numbers",
                ParsedInfoTestCase(
                    file = File("Wicket.Wicker.Potato.4.2023.UHD.BluRay.2160p.mkv"),
                    expectedCollection = "Wicket Wicker Potato 4",
                    expectedFileName = "Wicket Wicker Potato 4",
                    expectedSearchTitles = listOf("Wicket Wicker Potato 4")
                )
            ),
            Named.of(
                "Title with year in parentheses",
                ParsedInfoTestCase(
                    file = File("Amazing Potato (2022) 1080p BluRay.mkv"),
                    expectedCollection = "Amazing Potato",
                    expectedFileName = "Amazing Potato",
                    expectedSearchTitles = listOf("Amazing Potato")
                )
            ),
            Named.of(
                "Same",
                ParsedInfoTestCase(
                    file = File("/Dumb ways to die/S01E03-How to unlucky i am.mkv"),
                    expectedCollection = "Dumb ways to die",
                    expectedFileName = "Dumb ways to die - S01E03 - How to unlucky i am",
                    expectedSearchTitles = listOf(
                        "Dumb ways to die",
                    ),
                    expectedEpisodeInfo = MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                        episodeTitle = "How to unlucky i am",
                        episodeNumber = 3,
                        seasonNumber = 1
                    )
                )
            ),
            Named.of(
                "Underscores and mixed tags",
                ParsedInfoTestCase(
                    file = File("my_movie_title_2019_1080p_x264_YTS.mkv"),
                    expectedCollection = "my movie title",
                    expectedFileName = "my movie title (2019)",
                    expectedSearchTitles = listOf("my movie title (2019)", "my movie title")
                )
            ),
            Named.of(
                "Multiple bracketed groups and release tags",
                ParsedInfoTestCase(
                    file = File("[GROUP][WEBRip][YTS]Some.Movie.Title.720p.WEBRip.x264.AAC-[eztv].mkv"),
                    expectedCollection = "Some Movie Title",
                    expectedFileName = "Some Movie Title",
                    expectedSearchTitles = listOf("Some Movie Title")
                )
            ),
            Named.of(
                "Remux, PROPER, REPACK and extras",
                ParsedInfoTestCase(
                    file = File("Cool.Movie.2018.1080p.BluRay.REMUX.PROPER.REPACK.READNFO-GRP.mkv"),
                    expectedCollection = "Cool Movie",
                    expectedFileName = "Cool Movie",
                    expectedSearchTitles = listOf("Cool Movie")
                )
            ),
            Named.of(
                "Hyphens and multiple dashes",
                ParsedInfoTestCase(
                    file = File("Potato-Fields_-_A-Strange.Day-2017-HDTV-720p.mkv"),
                    expectedCollection = "Potato-Fields",
                    expectedFileName = "Potato-Fields - A-Strange Day",
                    expectedSearchTitles = listOf("Potato-Fields", "Potato-Fields - A-Strange Day")
                )
            ),
            Named.of(
                "Trailing group and site tags",
                ParsedInfoTestCase(
                    file = File("Movie.Name.2015.1080p.BluRay.x264-[YTS.MX].mkv"),
                    expectedCollection = "Movie Name",
                    expectedFileName = "Movie Name",
                    expectedSearchTitles = listOf("Movie Name")
                )
            ),
            Named.of(
                "IMAX and UNRATED markers",
                ParsedInfoTestCase(
                    file = File("Epic.Film.IMAX.UNRATED.2019.2160p.HDR.HEVC.mkv"),
                    expectedCollection = "Epic Film",
                    expectedFileName = "Epic Film",
                    expectedSearchTitles = listOf("Epic Film")
                )
            ),
            Named.of(
                "Sample and Trailer should be stripped",
                ParsedInfoTestCase(
                    file = File("Amazing.Movie.2020.1080p.Trailer-SAMPLE.mp4"),
                    expectedCollection = "Amazing Movie",
                    expectedFileName = "Amazing Movie",
                    expectedSearchTitles = listOf("Amazing Movie")
                )
            ),
            Named.of(
                "Parentheses director's cut",
                ParsedInfoTestCase(
                    file = File("The.Great.Film.(Director's.Cut).2016.1080p.BluRay.mkv"),
                    expectedCollection = "The Great Film",
                    expectedFileName = "The Great Film",
                    expectedSearchTitles = listOf("The Great Film")
                )
            ),
            Named.of(
                "Mixed separators and version tags",
                ParsedInfoTestCase(
                    file = File("Show.Name.S01.E02.720p.HDTV.x264-Group_v2.mkv"),
                    expectedCollection = "Show Name",
                    expectedFileName = "Show Name - S01E02",
                    expectedSearchTitles = listOf("Show Name"),
                    expectedEpisodeInfo = MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                        episodeNumber = 2,
                        seasonNumber = 1
                    )
                )
            ),
            Named.of(
                "Square brackets year and tags",
                ParsedInfoTestCase(
                    file = File("Title [2014] [1080p] [BluRay] [ENG].mkv"),
                    expectedCollection = "Title",
                    expectedFileName = "Title",
                    expectedSearchTitles = listOf("Title")
                )
            ),
            Named.of(
                "Version suffixes and fix tags",
                ParsedInfoTestCase(
                    file = File("Movie.Title.720p.HDTV.x264-FLEET.fix.mkv"),
                    expectedCollection = "Movie Title",
                    expectedFileName = "Movie Title",
                    expectedSearchTitles = listOf("Movie Title")
                )
            ),
            Named.of(
                "Nested brackets and group names",
                ParsedInfoTestCase(
                    file = File("[HD] (2020) Weird.Movie.Title - Extended.Edition [Group-Name].mkv"),
                    expectedCollection = "Weird Movie Title",
                    expectedFileName = "Weird Movie Title - Extended Edition",
                    expectedSearchTitles = listOf("Weird Movie Title", "Weird Movie Title - Extended Edition")
                )
            )
        )

        @JvmStatic
        fun parseVideoTypeCases() = listOf(
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

            // Additional parse/dumb filename cases
            Named.of(
                "Lowercase sXe pattern",
                ParseVideoTypeTestCase(
                    file = File("weird_show.s01e02.720p.mkv"),
                    expectedType = MediaType.Serie
                )
            ),
            Named.of(
                "Spaces and full words",
                ParseVideoTypeTestCase(
                    file = File("Some Show Season 02 Episode 09 1080p.mkv"),
                    expectedType = MediaType.Serie
                )
            ),
            Named.of(
                "1x02 style",
                ParseVideoTypeTestCase(
                    file = File("Show.Name.1x02.HDTV.mp4"),
                    expectedType = MediaType.Serie
                )
            ),
            Named.of(
                "Season and episode no separators",
                ParseVideoTypeTestCase(
                    file = File("ShowNameSeason03Episode04.avi"),
                    expectedType = MediaType.Serie
                )
            ),
            Named.of(
                "Movie with year and extra tags",
                ParseVideoTypeTestCase(
                    file = File("Some.Movie.Title.1999.720p.BluRay.x264-GROUP.mkv"),
                    expectedType = MediaType.Movie
                )
            ),
            Named.of(
                "Confusing underscores and trailers",
                ParseVideoTypeTestCase(
                    file = File("a_movie_trailer_2017_sample.mp4"),
                    expectedType = MediaType.Movie
                )
            ),
            Named.of(
                "Mixed separators and version tags",
                ParseVideoTypeTestCase(
                    file = File("Show.Name.S01.E02.720p.HDTV.x264-Group_v2.mkv"),
                    expectedType = MediaType.Serie
                )
            ),
        )
    }
}