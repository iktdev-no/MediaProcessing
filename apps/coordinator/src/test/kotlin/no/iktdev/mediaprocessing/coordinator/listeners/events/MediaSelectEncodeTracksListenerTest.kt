package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.data.Disposition
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.mediaprocessing.ffmpeg.data.Tags
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksEncodeSelectedEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MediaTracksEncodeSelectorTest: MediaSelectEncodeTracksListener() {

    private fun dummyAudioStream(
        index: Int,
        language: String,
        channels: Int,
        durationTs: Long = 1000
    ): AudioStream {
        return AudioStream(
            index = index,
            codec_name = "aac",
            codec_long_name = "AAC",
            codec_type = "audio",
            codec_tag_string = "",
            codec_tag = "",
            r_frame_rate = "0/0",
            avg_frame_rate = "0/0",
            time_base = "1/1000",
            start_pts = 0,
            start_time = "0",
            duration = null,
            duration_ts = durationTs,
            disposition = Disposition(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            tags = Tags(
                title = null, BPS = null, DURATION = null, NUMBER_OF_FRAMES = 0,
                NUMBER_OF_BYTES = null, _STATISTICS_WRITING_APP = null, _STATISTICS_WRITING_DATE_UTC = null,
                _STATISTICS_TAGS = null, language = language, filename = null, mimetype = null
            ),
            profile = "LC",
            sample_fmt = "fltp",
            sample_rate = "48000",
            channels = channels,
            channel_layout = "stereo",
            bits_per_sample = 0
        )
    }

    private fun dummyVideoStream(index: Int, durationTs: Long = 1000): VideoStream {
        return VideoStream(
            index = index,
            codec_name = "h264",
            codec_long_name = "H.264",
            codec_type = "video",
            codec_tag_string = "",
            codec_tag = "",
            r_frame_rate = "25/1",
            avg_frame_rate = "25/1",
            time_base = "1/1000",
            start_pts = 0,
            start_time = "0",
            duration = null,
            duration_ts = durationTs,
            disposition = Disposition(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            tags = Tags(
                title = null, BPS = null, DURATION = null, NUMBER_OF_FRAMES = 0,
                NUMBER_OF_BYTES = null, _STATISTICS_WRITING_APP = null, _STATISTICS_WRITING_DATE_UTC = null,
                _STATISTICS_TAGS = null, language = "eng", filename = null, mimetype = null
            ),
            profile = "main",
            width = 1920,
            height = 1080,
            coded_width = 1920,
            coded_height = 1080,
            closed_captions = 0,
            has_b_frames = 0,
            sample_aspect_ratio = "1:1",
            display_aspect_ratio = "16:9",
            pix_fmt = "yuv420p",
            level = 30,
            color_range = "tv",
            color_space = "bt709",
            color_transfer = "bt709",
            color_primaries = "bt709",
            chroma_location = "left",
            refs = 1
        )
    }

    @Test
    @DisplayName("""
        Hvis video streams har ulik varighet
        Når getVideoTrackToUse kalles
        Så:
            Returneres index til stream med lengst varighet
    """)
    fun testVideoTrackSelection() {
        val streams = listOf(dummyVideoStream(0, 1000), dummyVideoStream(1, 5000))
        val index = getVideoTrackToUse(streams)
        assertEquals(1, index)
    }

    @Test
    @DisplayName("""
        Hvis audio streams inneholder foretrukket språk jpn med 2 kanaler
        Når getAudioDefaultTrackToUse kalles
        Så:
            Returneres index til jpn stereo track
    """)
    fun testAudioDefaultTrackSelectionPreferredLanguageStereo() {
        val streams = listOf(
            dummyAudioStream(0, "eng", 2),
            dummyAudioStream(1, "jpn", 2),
            dummyAudioStream(2, "jpn", 6)
        )
        val index = getAudioDefaultTrackToUse(streams)
        assertEquals(1, index)
    }

    @Test
    @DisplayName("""
        Hvis audio streams inneholder foretrukket språk jpn med 6 kanaler
        Når getAudioExtendedTrackToUse kalles
        Så:
            Returneres index til jpn 6-kanals track
    """)
    fun testAudioExtendedTrackSelectionPreferredLanguageSurround() {
        val streams = listOf(
            dummyAudioStream(0, "jpn", 2),
            dummyAudioStream(1, "jpn", 6)
        )
        val defaultIndex = getAudioDefaultTrackToUse(streams)
        val extendedIndex = getAudioExtendedTrackToUse(streams, defaultIndex)
        assertEquals(0, defaultIndex)
        assertEquals(1, extendedIndex)
    }

    @Test
    @DisplayName("""
        Hvis audio streams ikke matcher foretrukket språk
        Når filterOnPreferredLanguage kalles
        Så:
            Returneres original liste uten filtrering
    """)
    fun testFilterOnPreferredLanguageFallback() {
        val streams = listOf(
            dummyAudioStream(0, "eng", 2),
            dummyAudioStream(1, "fra", 2)
        )
        val filtered = streams.filterOnPreferredLanguage()
        assertEquals(streams.size, filtered.size)
    }

    @Test
    @DisplayName("""
    Hvis audio streams ikke matcher foretrukket språk
    Når getAudioDefaultTrackToUse kalles
    Så:
        Velges et spor (fallback) selv om ingen matcher
    """)
    fun testAudioDefaultTrackFallbackSelection() {
        val streams = listOf(
            dummyAudioStream(0, "eng", 2),
            dummyAudioStream(1, "fra", 2)
        )

        // filterOnPreferredLanguage skal returnere original listen
        val filtered = streams.filterOnPreferredLanguage()
        assertEquals(streams.size, filtered.size)

        // getAudioDefaultTrackToUse skal likevel velge et spor
        val selectedIndex = getAudioDefaultTrackToUse(streams)

        // Sjekk at det faktisk er en gyldig index
        assertTrue(selectedIndex in streams.indices)

        // I dette tilfellet velges siste med høyest index (1)
        assertEquals(0, selectedIndex)
    }



    class DummyEvent: Event()

    @Test
    @DisplayName("""
        Hvis event ikke er MediaStreamParsedEvent
        Når onEvent kalles
        Så:
            Returneres null
    """)
    fun testOnEventNonParsedEvent() {
        val result = onEvent(DummyEvent(), emptyList())
        assertNull(result)
    }

    @Test
    @DisplayName("""
        Hvis event er MediaStreamParsedEvent med video og audio
        Når onEvent kalles
        Så:
            Returneres MediaTracksEncodeSelectedEvent med riktige spor
    """)
    fun testOnEventParsedEvent() {
        val videoStreams = listOf(dummyVideoStream(0, 1000))
        val audioStreams = listOf(dummyAudioStream(0, "jpn", 2), dummyAudioStream(1, "jpn", 6))
        val parsedEvent = MediaStreamParsedEvent(
            ParsedMediaStreams(videoStream = videoStreams, audioStream = audioStreams, subtitleStream = emptyList())
        )
        val result = onEvent(parsedEvent, emptyList()) as MediaTracksEncodeSelectedEvent
        assertEquals(0, result.selectedVideoTrack)
        assertEquals(0, result.selectedAudioTrack)
        assertEquals(1, result.selectedAudioExtendedTrack)
    }
}
