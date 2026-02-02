package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.FakeCoordinatorEnv
import no.iktdev.mediaprocessing.MockData
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksEncodeSelectedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class MediaTracksEncodeSelectorTest {

    private fun testPreference(): Preference {
        val tmp = File.createTempFile("pref", ".json")
        tmp.writeText(
            """
            {
              "processer": {},
              "language": {
                "preferredAudio": ["jpn", "eng"],
                "preferredSubtitles": ["eng"],
                "preferOriginal": true,
                "avoidDub": true,
                "subtitleFormatPriority": ["ass","srt","vtt","smi"],
                "subtitleSelectionMode": "DialogueOnly"
              }
            }
            """.trimIndent()
        )
        return Preference(FakeCoordinatorEnv(tmp))
    }

    private val listener = MediaSelectEncodeTracksListener(testPreference())

    // ------------------------------------------------------------
    // TESTS
    // ------------------------------------------------------------

    @Test
    @DisplayName("""
        Når video streams er tilgjengelige
        Hvis en stream har lengre varighet enn de andre
        Så:
         Velges video-sporet med lengst varighet
        """)
    fun testVideoTrackSelection() {
        val streams = listOf(
            MockData.dummyVideoStream(0, 1000),
            MockData.dummyVideoStream(1, 5000)
        )

        val event = MediaStreamParsedEvent(
            ParsedMediaStreams(videoStream = streams, audioStream = emptyList(), subtitleStream = emptyList())
        ).newReferenceId()

        val result = listener.onEvent(event, emptyList()) as MediaTracksEncodeSelectedEvent
        assertEquals(1, result.selectedVideoTrack)
    }

    @Test@DisplayName("""
        Når audio streams inneholder flere språk og kanaloppsett
        Hvis foretrukket språk finnes i stereo
        Så:
         Velges jpn stereo som default audio
        """)
    fun testAudioDefaultTrackSelectionPreferredLanguageStereo() {
        val audio = listOf(
            MockData.dummyAudioStream(0, "eng", 2),
            MockData.dummyAudioStream(1, "jpn", 2),
            MockData.dummyAudioStream(2, "jpn", 6)
        )

        val event = MediaStreamParsedEvent(
            ParsedMediaStreams(
                videoStream = listOf(MockData.dummyVideoStream(0)),
                audioStream = audio,
                subtitleStream = emptyList()
            )
        ).newReferenceId()

        val result = listener.onEvent(event, emptyList()) as MediaTracksEncodeSelectedEvent
        assertEquals(1, result.selectedAudioTrack)
    }

    @Test
    @DisplayName("""
        Når audio streams inneholder både stereo og surround
        Hvis foretrukket språk finnes i begge
        Så:
         Velges jpn 6-kanals som extended audio
        """)
    fun testAudioExtendedTrackSelectionPreferredLanguageSurround() {
        val audio = listOf(
            MockData.dummyAudioStream(0, "jpn", 2),
            MockData.dummyAudioStream(1, "jpn", 6)
        )

        val event = MediaStreamParsedEvent(
            ParsedMediaStreams(
                videoStream = listOf(MockData.dummyVideoStream(0)),
                audioStream = audio,
                subtitleStream = emptyList()
            )
        ).newReferenceId()

        val result = listener.onEvent(event, emptyList()) as MediaTracksEncodeSelectedEvent
        assertEquals(0, result.selectedAudioTrack)
        assertEquals(1, result.selectedAudioExtendedTrack)
    }

    class DummyEvent : Event()

    @Test
    @DisplayName("""
        Når event ikke er av typen MediaStreamParsedEvent
        Hvis onEvent kalles
        Så:
         Returneres null
        """)
    fun testOnEventNonParsedEvent() {
        assertNull(listener.onEvent(DummyEvent(), emptyList()))
    }

    @Test
    @DisplayName("""
        Når MediaStreamParsedEvent mottas med video og audio streams
        Hvis sporene analyseres etter preferanser
        Så:
         Velges riktige video-, default audio- og extended audio-spor
        """)
    fun testOnEventParsedEvent() {
        val video = listOf(MockData.dummyVideoStream(0, 1000))
        val audio = listOf(
            MockData.dummyAudioStream(0, "jpn", 2),
            MockData.dummyAudioStream(1, "jpn", 6)
        )

        val parsed = MediaStreamParsedEvent(
            ParsedMediaStreams(videoStream = video, audioStream = audio, subtitleStream = emptyList())
        ).newReferenceId()

        val result = listener.onEvent(parsed, emptyList()) as MediaTracksEncodeSelectedEvent

        assertEquals(0, result.selectedVideoTrack)
        assertEquals(0, result.selectedAudioTrack)
        assertEquals(1, result.selectedAudioExtendedTrack)
    }
}
