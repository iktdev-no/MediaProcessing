package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.FakeCoordinatorEnv
import no.iktdev.mediaprocessing.MockData
import no.iktdev.mediaprocessing.MockData.dummyAudioStream
import no.iktdev.mediaprocessing.MockData.dummyDisposition
import no.iktdev.mediaprocessing.MockData.dummyTags
import no.iktdev.mediaprocessing.MockData.dummyVideoStream
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksEncodeSelectedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
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
    // VIDEO SELECTION
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

    // ------------------------------------------------------------
    // MULTI-LANGUAGE AUDIO SELECTION
    // ------------------------------------------------------------

    @Test
    @DisplayName("""
        Når audio streams inneholder flere språk og kanaloppsett
        Hvis foretrukket språk finnes i både stereo og surround
        Så:
         Velges default = stereo og extended = surround for hvert språk
    """)
    fun testMultiLanguageSelection() {
        val audio = listOf(
            MockData.dummyAudioStream(0, "eng", 2),
            MockData.dummyAudioStream(1, "eng", 6),
            MockData.dummyAudioStream(2, "jpn", 2),
            MockData.dummyAudioStream(3, "jpn", 6)
        )

        val event = MediaStreamParsedEvent(
            ParsedMediaStreams(
                videoStream = listOf(MockData.dummyVideoStream(0)),
                audioStream = audio,
                subtitleStream = emptyList()
            )
        ).newReferenceId()

        val result = listener.onEvent(event, emptyList()) as MediaTracksEncodeSelectedEvent

        assertEquals(2, result.audioTracks.size)

        val jpn = result.audioTracks[0]
        assertEquals("jpn", jpn.language)
        assertEquals(2, jpn.defaultListIndex)
        assertEquals(3, jpn.extendedListIndex)

        val eng = result.audioTracks[1]
        assertEquals("eng", eng.language)
        assertEquals(0, eng.defaultListIndex)
        assertEquals(1, eng.extendedListIndex)
    }

    @Test
    @DisplayName("""
        Når et språk kun finnes i stereo
        Hvis extended ikke finnes
        Så:
         Skal extendedListIndex være null
    """)
    fun testExtendedMissing() {
        val audio = listOf(
            MockData.dummyAudioStream(0, "jpn", 2),
            MockData.dummyAudioStream(1, "eng", 2)
        )

        val event = MediaStreamParsedEvent(
            ParsedMediaStreams(
                videoStream = listOf(MockData.dummyVideoStream(0)),
                audioStream = audio,
                subtitleStream = emptyList()
            )
        ).newReferenceId()

        val result = listener.onEvent(event, emptyList()) as MediaTracksEncodeSelectedEvent

        assertEquals(2, result.audioTracks.size)

        val jpn = result.audioTracks[0]
        assertEquals("jpn", jpn.language)
        assertEquals(0, jpn.defaultListIndex)
        assertNull(jpn.extendedListIndex)

        val eng = result.audioTracks[1]
        assertEquals("eng", eng.language)
        assertEquals(1, eng.defaultListIndex)
        assertNull(eng.extendedListIndex)
    }

    @Test
    @DisplayName("""
        Når foretrukket språk ikke finnes i filen
        Hvis kun ett av språkene finnes
        Så:
         Skal kun eksisterende språk returneres
    """)
    fun testMissingLanguageIgnored() {
        val audio = listOf(
            MockData.dummyAudioStream(0, "eng", 2)
        )

        val event = MediaStreamParsedEvent(
            ParsedMediaStreams(
                videoStream = listOf(MockData.dummyVideoStream(0)),
                audioStream = audio,
                subtitleStream = emptyList()
            )
        ).newReferenceId()

        val result = listener.onEvent(event, emptyList()) as MediaTracksEncodeSelectedEvent

        assertEquals(1, result.audioTracks.size)
        assertEquals("eng", result.audioTracks[0].language)
    }

    @Test
    @DisplayName("""
        Når original-spor finnes
        Hvis preferOriginal = true
        Så:
         Velges original-sporet som default
    """)
    fun testPreferOriginal() {
        val audio = listOf(
            MockData.dummyAudioStream(
                0, "jpn", 2,
                disposition = dummyDisposition { original = true }
            ),
            MockData.dummyAudioStream(1, "jpn", 6),
            MockData.dummyAudioStream(2, "eng", 2),
            MockData.dummyAudioStream(
                3, "eng", 6,
                disposition = dummyDisposition { original = true }
            )
        )

        val event = MediaStreamParsedEvent(
            ParsedMediaStreams(
                videoStream = listOf(MockData.dummyVideoStream(0)),
                audioStream = audio,
                subtitleStream = emptyList()
            )
        ).newReferenceId()

        val result = listener.onEvent(event, emptyList()) as MediaTracksEncodeSelectedEvent

        val jpn = result.audioTracks[0]
        assertEquals(0, jpn.defaultListIndex)

        val eng = result.audioTracks[1]
        assertEquals(3, eng.extendedListIndex)
    }

    @Test
    @DisplayName("""
        Når dub-spor finnes
        Hvis avoidDub = true
        Så:
         Skal dub-spor ignoreres i både default og extended
    """)
    fun testAvoidDub() {
        val audio = listOf(
            MockData.dummyAudioStream(
                0, "jpn", 2,
                disposition = dummyDisposition { dub = true }
            ),
            MockData.dummyAudioStream(1, "jpn", 2),
            MockData.dummyAudioStream(
                2, "jpn", 6,
                disposition = dummyDisposition { dub = true }
            ),
            MockData.dummyAudioStream(3, "jpn", 6)
        )

        val event = MediaStreamParsedEvent(
            ParsedMediaStreams(
                videoStream = listOf(MockData.dummyVideoStream(0)),
                audioStream = audio,
                subtitleStream = emptyList()
            )
        ).newReferenceId()

        val result = listener.onEvent(event, emptyList()) as MediaTracksEncodeSelectedEvent

        val jpn = result.audioTracks[0]
        assertEquals(1, jpn.defaultListIndex)
        assertEquals(3, jpn.extendedListIndex)
    }

    @Test
    @DisplayName("""
    Når ingen foretrukne språk finnes
    Hvis original-spor finnes og preferOriginal = true
    Så:
        Skal original-språket beholdes som default
""")
    fun testFallbackToOriginalWhenNoPreferredLanguageMatches() {
        val audio = listOf(
            // Svensk stereo, original
            dummyAudioStream(
                index = 0,
                channels = 2,
                disposition = dummyDisposition { original = true },
                tags = dummyTags(language = "swe")
            ),
            // Dansk dub 5.1
            dummyAudioStream(
                index = 1,
                channels = 6,
                disposition = dummyDisposition(),
                tags = dummyTags(language = "dan")
            )
        )

        // Her antar vi at preferansen din er satt opp et sted som:
        // preferredLanguages = listOf("eng")
        // preferOriginal = true

        val parsed = MediaStreamParsedEvent(
            ParsedMediaStreams(
                videoStream = listOf(dummyVideoStream(index = 0)),
                audioStream = audio,
                subtitleStream = emptyList()
            )
        ).newReferenceId()

        val result = listener.onEvent(parsed, emptyList()) as MediaTracksEncodeSelectedEvent

        // Vi forventer at svensk original slipper gjennom
        val swe = result.audioTracks.firstOrNull { it.language.startsWith("sw") || it.language == "swe" }
            ?: error("Forventet at svensk språkgruppe skulle finnes")

        assertEquals(0, swe.defaultListIndex)
        assertNull(swe.extendedListIndex)
    }

    @Test
    @DisplayName("""
        Når engelsk er dub og japansk er original
        Hvis avoidDub = true og preferOriginal = true
        Så:
            Skal japansk velges og engelsk ignoreres
    """)
    fun testEngDubJpnOriginal() {
        val audio = listOf(
            // Japansk original stereo
            dummyAudioStream(
                index = 0,
                channels = 2,
                disposition = dummyDisposition { original = true },
                tags = dummyTags(language = "jpn")
            ),
            // Engelsk dub stereo
            dummyAudioStream(
                index = 1,
                channels = 2,
                disposition = dummyDisposition { dub = true },
                tags = dummyTags(language = "eng")
            )
        )

        val event = MediaStreamParsedEvent(
            ParsedMediaStreams(
                videoStream = listOf(dummyVideoStream(0)),
                audioStream = audio,
                subtitleStream = emptyList()
            )
        ).newReferenceId()

        val result = listener.onEvent(event, emptyList()) as MediaTracksEncodeSelectedEvent

        // Kun japansk skal være med
        assertEquals(1, result.audioTracks.size)

        val jpn = result.audioTracks[0]
        assertEquals("jpn", jpn.language)
        assertEquals(0, jpn.defaultListIndex)
        assertNull(jpn.extendedListIndex)
    }


    // ------------------------------------------------------------
    // EVENT TYPE CHECK
    // ------------------------------------------------------------

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
}
