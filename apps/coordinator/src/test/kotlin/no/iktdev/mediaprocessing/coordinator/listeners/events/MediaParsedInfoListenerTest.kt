package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.coordinator.parse.evaluateMediaType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MediaParsedInfoListenerTest : TestBase() {
    private val listener = MediaParsedInfoListener()

    // --- Hjelpemetode for å redusere duplisert testlogikk ---
    private fun assertParsedInfo(
        file: IFile,
        expectedCollection: String,
        expectedFileName: String,
        expectedSearchTitles: List<String>,
        expectedEpisodeInfo: MediaParsedInfoEvent.ParsedData.EpisodeInfo? = null
    ) {
        val startEvent = StartProcessingEvent(data = StartData(
            operation = emptySet(),
            flow = StartFlow.Auto,
            fileUri = file.absolutePath
        )).newReferenceId()

        val result = listener.onEvent(startEvent, history) as? MediaParsedInfoEvent
        assertThat(result).isNotNull
        assertThat(result!!.data).isNotNull
        assertThat(result.data.parsedCollection).isEqualTo(expectedCollection)
        assertThat(result.data.parsedFileName).isEqualTo(expectedFileName)
        assertThat(result.data.parsedSearchTitles).isEqualTo(expectedSearchTitles)
    }

    // ==========================================
    // DEL 1: Parser info tester (Individuelle)
    // ==========================================

    @Test
    fun `Serie with year in the future`() {
        assertParsedInfo(
            file = IFile("Demon Lord 2099 - S01E01 - Cyberpunk City Shinjuku.mkv"),
            expectedCollection = "Demon Lord 2099",
            expectedFileName = "Demon Lord 2099 - S01E01 - Cyberpunk City Shinjuku",
            expectedSearchTitles = listOf("Demon Lord 2099"),
            expectedEpisodeInfo = MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                seasonNumber = 1,
                episodeNumber = 1,
                episodeTitle = "Cyberpunk City Shinjuku"
            )
        )
    }

    @Test
    fun `Series episode parsing`() {
        assertParsedInfo(
            file = IFile("Fancy.Thomas.S03E03.Enemy.1080p.AMAZING.WEB-VALUE.DDP5AN.1.H.264.mkv"),
            expectedCollection = "Fancy Thomas",
            expectedFileName = "Fancy Thomas - S03E03 - Enemy",
            expectedSearchTitles = listOf("Fancy Thomas"),
            expectedEpisodeInfo = MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                seasonNumber = 3,
                episodeNumber = 3,
                episodeTitle = "Enemy"
            )
        )
    }

    @Test
    fun `Movie parsing with year`() {
        assertParsedInfo(
            file = IFile("Epic.Potato.Movie.2021.1080p.BluRay.x264.mkv"),
            expectedCollection = "Epic Potato Movie",
            expectedFileName = "Epic Potato Movie",
            expectedSearchTitles = listOf("Epic Potato Movie")
        )
    }

    @Test
    fun `Series with dots and special characters`() {
        assertParsedInfo(
            file = IFile("Like.a.Potato.Chef.S01E01.Departure.\\u0026.Skills.1080p.Potato.mkv"),
            expectedCollection = "Like a Potato Chef",
            expectedFileName = "Like a Potato Chef - S01E01 - Departure \\u0026 Skills",
            expectedSearchTitles = listOf("Like a Potato Chef"),
            expectedEpisodeInfo = MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                seasonNumber = 1,
                episodeNumber = 1,
                episodeTitle = "Departure \\u0026 Skills"
            )
        )
    }

    @Test
    fun `Movie with extended title`() {
        assertParsedInfo(
            file = IFile("Potato-Pass Movie - Skinke.mkv"),
            expectedCollection = "Potato-Pass Movie",
            expectedFileName = "Potato-Pass Movie - Skinke",
            expectedSearchTitles = listOf("Potato-Pass Movie", "Potato-Pass Movie - Skinke")
        )
    }

    @Test
    fun `Name with numbers`() {
        assertParsedInfo(
            file = IFile("[TST] Fancy Name Test 99 - 01 [Nans][#00A8E6].mkv"),
            expectedCollection = "Fancy Name Test 99",
            expectedFileName = "Fancy Name Test 99 - 01",
            expectedSearchTitles = listOf("Fancy Name Test 99", "Fancy Name Test 99 - 01")
        )
    }

    @Test
    fun `Movie name with numbers`() {
        assertParsedInfo(
            file = IFile("Wicket.Wicker.Potato.4.2023.UHD.BluRay.2160p.mkv"),
            expectedCollection = "Wicket Wicker Potato 4",
            expectedFileName = "Wicket Wicker Potato 4",
            expectedSearchTitles = listOf("Wicket Wicker Potato 4")
        )
    }

    @Test
    fun `Title with year in parentheses`() {
        assertParsedInfo(
            file = IFile("Amazing Potato (2022) 1080p BluRay.mkv"),
            expectedCollection = "Amazing Potato",
            expectedFileName = "Amazing Potato",
            expectedSearchTitles = listOf("Amazing Potato")
        )
    }

    @Test
    fun `Same`() {
        assertParsedInfo(
            file = IFile("/Dumb ways to die/S01E03-How to unlucky i am.mkv"),
            expectedCollection = "Dumb ways to die",
            expectedFileName = "Dumb ways to die - S01E03 - How to unlucky i am",
            expectedSearchTitles = listOf("Dumb ways to die"),
            expectedEpisodeInfo = MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                episodeTitle = "How to unlucky i am",
                episodeNumber = 3,
                seasonNumber = 1
            )
        )
    }

    @Test
    fun `Correctly extracted and cleaned`() {
        assertParsedInfo(
            file = IFile("store:///Dumb ways to die S01 1080p EXMAS POTATO MASTER/S01E03-How to unlucky i am.mkv"),
            expectedCollection = "Dumb ways to die",
            expectedFileName = "Dumb ways to die - S01E03 - How to unlucky i am",
            expectedSearchTitles = listOf("Dumb ways to die"),
            expectedEpisodeInfo = MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                episodeTitle = "How to unlucky i am",
                episodeNumber = 3,
                seasonNumber = 1
            )
        )
    }

    @Test
    fun `Underscores and mixed tags`() {
        assertParsedInfo(
            file = IFile("my_movie_title_2019_1080p_x264_YTS.mkv"),
            expectedCollection = "my movie title",
            expectedFileName = "my movie title (2019)",
            expectedSearchTitles = listOf("my movie title (2019)", "my movie title")
        )
    }

    @Test
    fun `Multiple bracketed groups and release tags`() {
        assertParsedInfo(
            file = IFile("[GROUP][WEBRip][YTS]Some.Movie.Title.720p.WEBRip.x264.AAC-[eztv].mkv"),
            expectedCollection = "Some Movie Title",
            expectedFileName = "Some Movie Title",
            expectedSearchTitles = listOf("Some Movie Title")
        )
    }

    @Test
    fun `Remux, PROPER, REPACK and extras`() {
        assertParsedInfo(
            file = IFile("Cool.Movie.2018.1080p.BluRay.REMUX.PROPER.REPACK.READNFO-GRP.mkv"),
            expectedCollection = "Cool Movie",
            expectedFileName = "Cool Movie",
            expectedSearchTitles = listOf("Cool Movie")
        )
    }

    @Test
    fun `Hyphens and multiple dashes`() {
        assertParsedInfo(
            file = IFile("Potato-Fields_-_A-Strange.Day-2017-HDTV-720p.mkv"),
            expectedCollection = "Potato-Fields",
            expectedFileName = "Potato-Fields - A-Strange Day",
            expectedSearchTitles = listOf("Potato-Fields", "Potato-Fields - A-Strange Day")
        )
    }

    @Test
    fun `Trailing group and site tags`() {
        assertParsedInfo(
            file = IFile("Movie.Name.2015.1080p.BluRay.x264-[YTS.MX].mkv"),
            expectedCollection = "Movie Name",
            expectedFileName = "Movie Name",
            expectedSearchTitles = listOf("Movie Name")
        )
    }

    @Test
    fun `IMAX and UNRATED markers`() {
        assertParsedInfo(
            file = IFile("Epic.Film.IMAX.UNRATED.2019.2160p.HDR.HEVC.mkv"),
            expectedCollection = "Epic Film",
            expectedFileName = "Epic Film",
            expectedSearchTitles = listOf("Epic Film")
        )
    }

    @Test
    fun `Sample and Trailer should be stripped`() {
        assertParsedInfo(
            file = IFile("Amazing.Movie.2020.1080p.Trailer-SAMPLE.mp4"),
            expectedCollection = "Amazing Movie",
            expectedFileName = "Amazing Movie",
            expectedSearchTitles = listOf("Amazing Movie")
        )
    }

    @Test
    fun `Parentheses director's cut`() {
        assertParsedInfo(
            file = IFile("The.Great.Film.(Director's.Cut).2016.1080p.BluRay.mkv"),
            expectedCollection = "The Great Film",
            expectedFileName = "The Great Film",
            expectedSearchTitles = listOf("The Great Film")
        )
    }

    @Test
    fun `Mixed separators and version tags for info`() {
        assertParsedInfo(
            file = IFile("Show.Name.S01.E02.720p.HDTV.x264-Group_v2.mkv"),
            expectedCollection = "Show Name",
            expectedFileName = "Show Name - S01E02",
            expectedSearchTitles = listOf("Show Name"),
            expectedEpisodeInfo = MediaParsedInfoEvent.ParsedData.EpisodeInfo(
                episodeNumber = 2,
                seasonNumber = 1
            )
        )
    }

    @Test
    fun `Square brackets year and tags`() {
        assertParsedInfo(
            file = IFile("Title [2014] [1080p] [BluRay] [ENG].mkv"),
            expectedCollection = "Title",
            expectedFileName = "Title",
            expectedSearchTitles = listOf("Title")
        )
    }

    @Test
    fun `Version suffixes and fix tags`() {
        assertParsedInfo(
            file = IFile("Movie.Title.720p.HDTV.x264-FLEET.fix.mkv"),
            expectedCollection = "Movie Title",
            expectedFileName = "Movie Title",
            expectedSearchTitles = listOf("Movie Title")
        )
    }

    @Test
    fun `Nested brackets and group names`() {
        assertParsedInfo(
            file = IFile("[HD] (2020) Weird.Movie.Title - Extended.Edition [Group-Name].mkv"),
            expectedCollection = "Weird Movie Title",
            expectedFileName = "Weird Movie Title - Extended Edition",
            expectedSearchTitles = listOf("Weird Movie Title", "Weird Movie Title - Extended Edition")
        )
    }


    // ==========================================
    // DEL 2: Parse Video Type tester (Individuelle)
    // ==========================================

    private fun assertVideoType(file: IFile, expectedType: MediaType) {
        val mediaType = file.evaluateMediaType()
        assertThat(mediaType).isEqualTo(expectedType)
    }

    @Test
    fun `Series file detection full block`() {
        assertVideoType(IFile("Fancy.Thomas.S03E03.Enemy.1080p.AMAZING.WEB-VALUE.DDP5AN.1.H.264.mkv"), MediaType.Serie)
    }

    @Test
    fun `Series file detection fully spelt`() {
        assertVideoType(IFile("Potato harvesting Season 1 Episode 5 720p WEB-DL.mkv"), MediaType.Serie)
    }

    @Test
    fun `Series file shorthand S full e`() {
        assertVideoType(IFile("Potato harvesting S1 - Episode 5 720p WEB-DL.mkv"), MediaType.Serie)
    }

    @Test
    fun `Series file shorthand S and e`() {
        assertVideoType(IFile("Potato harvesting S1 - E5 720p WEB-DL.mkv"), MediaType.Serie)
    }

    @Test
    fun `Movie file detection`() {
        assertVideoType(IFile("Epic.Potato.Movie.2021.1080p.BluRay.x264.mkv"), MediaType.Movie)
    }

    @Test
    fun `Lowercase sXe pattern`() {
        assertVideoType(IFile("weird_show.s01e02.720p.mkv"), MediaType.Serie)
    }

    @Test
    fun `Spaces and full words`() {
        assertVideoType(IFile("Some Show Season 02 Episode 09 1080p.mkv"), MediaType.Serie)
    }

    @Test
    fun `1x02 style`() {
        assertVideoType(IFile("Show.Name.1x02.HDTV.mp4"), MediaType.Serie)
    }

    @Test
    fun `Season and episode no separators`() {
        assertVideoType(IFile("ShowNameSeason03Episode04.avi"), MediaType.Serie)
    }

    @Test
    fun `Movie with year and extra tags`() {
        assertVideoType(IFile("Some.Movie.Title.1999.720p.BluRay.x264-GROUP.mkv"), MediaType.Movie)
    }

    @Test
    fun `Confusing underscores and trailers`() {
        assertVideoType(IFile("a_movie_trailer_2017_sample.mp4"), MediaType.Movie)
    }

    @Test
    fun `Mixed separators and version tags for type`() {
        assertVideoType(IFile("Show.Name.S01.E02.720p.HDTV.x264-Group_v2.mkv"), MediaType.Serie)
    }
}