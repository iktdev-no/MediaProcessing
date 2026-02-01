package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleTags
import no.iktdev.mediaprocessing.ffmpeg.data.Tags
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksDetermineSubtitleTypeEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksExtractSelectedEvent
import no.iktdev.mediaprocessing.shared.common.model.SubtitleItem
import no.iktdev.mediaprocessing.shared.common.model.SubtitleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MediaSelectExtractTracksListenerTest: TestBase() {


    // Vi lager en subclass som gir oss tilgang til alt og lar oss overstyre språkpreferanser
    class TestableMediaSelectExtractTracksListener(
        private val preferredLanguages: Set<String> = emptySet()
    ) : MediaSelectExtractTracksListener() {
        override fun limitToLanguages(): Set<String> = preferredLanguages
        // gjør private extension tilgjengelig via wrapper
        fun callFilterOnPreferredLanguage(streams: List<SubtitleStream>): List<SubtitleStream> {
            return streams.filterOnPreferredLanguage()
        }
    }

    private fun dummySubtitleStream(index: Int, language: String?, type: SubtitleType): SubtitleItem {
        val stream = SubtitleStream(
            index = index,
            codec_name = "ass",
            codec_long_name = "ASS",
            codec_type = "subtitle",
            codec_tag_string = "",
            codec_tag = "",
            r_frame_rate = "0/0",
            avg_frame_rate = "0/0",
            time_base = "1/1000",
            start_pts = 0,
            start_time = "0",
            duration = null,
            duration_ts = 1000,
            disposition = null,
            tags = Tags(
                title = null, BPS = null, DURATION = null, NUMBER_OF_FRAMES = 0,
                NUMBER_OF_BYTES = null, _STATISTICS_WRITING_APP = null, _STATISTICS_WRITING_DATE_UTC = null,
                _STATISTICS_TAGS = null, language = language, filename = null, mimetype = null
            ),
            subtitle_tags = SubtitleTags(language = language, filename = null, mimetype = null)
        )
        return SubtitleItem(stream = stream, type = type)
    }

    @Test
    @DisplayName("""
        Hvis event ikke er MediaTracksDetermineSubtitleTypeEvent
        Når onEvent kalles
        Så:
            Returneres null
    """)
    fun testOnEventNonSubtitleEvent() {
        val listener = TestableMediaSelectExtractTracksListener()
        val result = listener.onEvent(DummyEvent(), emptyList())
        assertNull(result)
    }

    @Test
    @DisplayName("""
        Hvis event inneholder Dialogue subtitles
        Når onEvent kalles
        Så:
            Returneres MediaTracksExtractSelectedEvent med index til Dialogue tracks
    """)
    fun testOnEventDialogueTracksSelected() {
        val listener = TestableMediaSelectExtractTracksListener()
        val items = listOf(
            dummySubtitleStream(0, "eng", SubtitleType.Dialogue),
            dummySubtitleStream(1, "eng", SubtitleType.Commentary)
        )
        val event = MediaTracksDetermineSubtitleTypeEvent(subtitleTrackItems = items).newReferenceId()
        val result = listener.onEvent(event, emptyList()) as MediaTracksExtractSelectedEvent
        assertEquals(listOf(0), result.selectedSubtitleTracks)
    }

    @Test
    @DisplayName("""
        Hvis limitToLanguages returnerer jpn
        Når filterOnPreferredLanguage kalles
        Så:
            Returneres kun spor med språk jpn
    """)
    fun testFilterOnPreferredLanguageWithLimit() {
        val listener = TestableMediaSelectExtractTracksListener(setOf("jpn"))
        val streams = listOf(
            dummySubtitleStream(0, "eng", SubtitleType.Dialogue).stream,
            dummySubtitleStream(1, "jpn", SubtitleType.Dialogue).stream
        )
        val filtered = listener.callFilterOnPreferredLanguage(streams)
        assertEquals(1, filtered.size)
        assertEquals("jpn", filtered[0].tags.language)
    }

    @Test
    @DisplayName("""
        Hvis limitToLanguages er tom
        Når filterOnPreferredLanguage kalles
        Så:
            Returneres original liste uten filtrering
    """)
    fun testFilterOnPreferredLanguageNoLimit() {
        val listener = TestableMediaSelectExtractTracksListener()
        val streams = listOf(
            dummySubtitleStream(0, "eng", SubtitleType.Dialogue).stream,
            dummySubtitleStream(1, "fra", SubtitleType.Dialogue).stream
        )
        val filtered = listener.callFilterOnPreferredLanguage(streams)
        assertEquals(streams.size, filtered.size)
    }

}