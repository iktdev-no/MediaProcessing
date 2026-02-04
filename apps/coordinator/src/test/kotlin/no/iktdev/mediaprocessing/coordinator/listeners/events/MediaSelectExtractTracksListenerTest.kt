package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.ZDS
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.FakeCoordinatorEnv
import no.iktdev.mediaprocessing.Files
import no.iktdev.mediaprocessing.MockData
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.getContent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksDetermineSubtitleTypeEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksExtractSelectedEvent
import no.iktdev.mediaprocessing.shared.common.model.SubtitleItem
import no.iktdev.mediaprocessing.shared.common.model.SubtitleType
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.SubtitleSelectionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class MediaSelectExtractTracksListenerTest {

    // ------------------------------------------------------------
    // Helper: create a Preference with custom subtitle settings
    // ------------------------------------------------------------
    private fun testPreference(
        preferredSubtitles: List<String> = listOf("eng"),
        formatPriority: List<String> = listOf("ass", "srt", "vtt", "smi"),
        mode: SubtitleSelectionMode = SubtitleSelectionMode.DialogueOnly
    ): Preference {
        val tmp = File.createTempFile("pref", ".json")
        tmp.writeText(
            """
            {
              "processer": {},
              "language": {
                "preferredAudio": ["eng"],
                "preferredSubtitles": ${preferredSubtitles},
                "preferOriginal": true,
                "avoidDub": true,
                "subtitleFormatPriority": ${formatPriority},
                "subtitleSelectionMode": "$mode"
              }
            }
            """.trimIndent()
        )
        return Preference(FakeCoordinatorEnv(tmp))
    }

    private fun listener(
        preferredSubtitles: List<String> = listOf("eng"),
        formatPriority: List<String> = listOf("ass", "srt", "vtt", "smi"),
        mode: SubtitleSelectionMode = SubtitleSelectionMode.DialogueOnly
    ) = MediaSelectExtractTracksListener(
        testPreference(preferredSubtitles, formatPriority, mode)
    )

    class DummyEvent : Event()

    // ------------------------------------------------------------
    // TESTS
    // ------------------------------------------------------------

    @Test
    @DisplayName("""
        Når event ikke er av typen MediaTracksDetermineSubtitleTypeEvent
        Hvis onEvent kalles
        Så:
         Returneres null
        """)
    fun testOnEventNonSubtitleEvent() {
        val result = listener().onEvent(DummyEvent(), emptyList())
        assertNull(result)
    }


    @Test
    @DisplayName("""
        Når subtitles inneholder Dialogue og Commentary
        Hvis modus er DialogueOnly
        Så:
         Velges kun Dialogue subtitles
        """)
    fun testDialogueSelection() {
        val items = listOf(
            SubtitleItem(MockData.dummySubtitleStream(0, "eng"), SubtitleType.Dialogue),
            SubtitleItem(MockData.dummySubtitleStream(1, "eng"), SubtitleType.Commentary)
        )

        val event = MediaTracksDetermineSubtitleTypeEvent(items).newReferenceId()
        val result = listener().onEvent(event, emptyList()) as MediaTracksExtractSelectedEvent

        assertEquals(listOf(0), result.selectedSubtitleTracks)
    }


    @Test
    @DisplayName("""
        Når subtitles finnes i flere språk
        Hvis foretrukket språk er jpn
        Så:
         Velges kun subtitles i jpn
        """)
    fun testPreferredLanguageSelection() {
        val items = listOf(
            SubtitleItem(MockData.dummySubtitleStream(0, "eng"), SubtitleType.Dialogue),
            SubtitleItem(MockData.dummySubtitleStream(1, "jpn"), SubtitleType.Dialogue)
        )

        val event = MediaTracksDetermineSubtitleTypeEvent(items).newReferenceId()
        val result = listener(preferredSubtitles = listOf("jpn"))
            .onEvent(event, emptyList()) as MediaTracksExtractSelectedEvent

        assertEquals(listOf(1), result.selectedSubtitleTracks)
    }


    @Test
    @DisplayName("""
        Når flere subtitles i samme språk finnes
        Hvis format-prioritet er ass > srt
        Så:
         Velges subtitle med codec ass
        """)
    fun testFormatPrioritySelection() {
        val items = listOf(
            SubtitleItem(MockData.dummySubtitleStream(0, "eng").copy(codec_name = "srt"), SubtitleType.Dialogue),
            SubtitleItem(MockData.dummySubtitleStream(1, "eng").copy(codec_name = "ass"), SubtitleType.Dialogue)
        )

        val event = MediaTracksDetermineSubtitleTypeEvent(items).newReferenceId()
        val result = listener(
            preferredSubtitles = listOf("eng"),
            formatPriority = listOf("ass", "srt")
        ).onEvent(event, emptyList()) as MediaTracksExtractSelectedEvent

        assertEquals(listOf(1), result.selectedSubtitleTracks)
    }


    @Test
    @DisplayName("""
        Når flere subtitles i samme språk finnes
        Hvis kun én subtitle per språk skal velges
        Så:
         Returneres kun ett spor
        """)
    fun testUniquePerLanguage() {
        val items = listOf(
            SubtitleItem(MockData.dummySubtitleStream(0, "eng").copy(codec_name = "srt"), SubtitleType.Dialogue),
            SubtitleItem(MockData.dummySubtitleStream(1, "eng").copy(codec_name = "vtt"), SubtitleType.Dialogue)
        )

        val event = MediaTracksDetermineSubtitleTypeEvent(items).newReferenceId()
        val result = listener().onEvent(event, emptyList()) as MediaTracksExtractSelectedEvent

        assertEquals(1, result.selectedSubtitleTracks.size)
    }

    @Test
    @DisplayName("""
    Når foretrukket språk er 'nor'
    Og tilgjengelige subtitles er både 'nob' og 'nno'
    Så:
      Skal 'nor' matche begge,
      og ett spor per språk returneres
""")
    fun testNorMacroLanguageSelection() {
        val items = listOf(
            SubtitleItem(
                MockData.dummySubtitleStream(0, "nob").copy(codec_name = "srt"),
                SubtitleType.Dialogue
            ),
            SubtitleItem(
                MockData.dummySubtitleStream(1, "nno").copy(codec_name = "ass"),
                SubtitleType.Dialogue
            )
        )

        val event = MediaTracksDetermineSubtitleTypeEvent(items).newReferenceId()

        val result = listener(
            preferredSubtitles = listOf("nor"),
            formatPriority = listOf("ass", "srt")
        ).onEvent(event, emptyList()) as MediaTracksExtractSelectedEvent

        // Vi forventer ett spor per språk: både nob og nno skal være med
        assertEquals(listOf(0, 1), result.selectedSubtitleTracks)
    }

    @Test
    @DisplayName("""
    Når foretrukket språk er 'nob'
    Og tilgjengelige subtitles er 'eng' og 'nno'
    Og 'eng' er originalspråk
    Så:
      Skal kun 'eng' velges
    """)
    fun testNobPreferenceFallsBackToOriginalEng() {
        val eng = MockData.dummySubtitleStream(0, "eng").copy(
            disposition = MockData.dummyDisposition {
                original = true
            }
        )
        val nno = MockData.dummySubtitleStream(1, "nno")

        val items = listOf(
            SubtitleItem(eng, SubtitleType.Dialogue),
            SubtitleItem(nno, SubtitleType.Dialogue)
        )

        val event = MediaTracksDetermineSubtitleTypeEvent(items).newReferenceId()

        val result = listener(
            preferredSubtitles = listOf("nob"), // no match
            mode = SubtitleSelectionMode.DialogueOnly
        ).onEvent(event, emptyList()) as MediaTracksExtractSelectedEvent

        assertEquals(listOf(0), result.selectedSubtitleTracks)
    }

    @Test
    @DisplayName("""
    Når foretrukket språk er 'nor'
    Og tilgjengelige subtitles inneholder 'nob'
    Så:
      Skal 'nor' matche 'nob'
    """)
    fun testNorMatchesNob() {
        val nob = MockData.dummySubtitleStream(0, "nob")

        val items = listOf(
            SubtitleItem(nob, SubtitleType.Dialogue)
        )

        val event = MediaTracksDetermineSubtitleTypeEvent(items).newReferenceId()

        val result = listener(
            preferredSubtitles = listOf("nor")
        ).onEvent(event, emptyList()) as MediaTracksExtractSelectedEvent

        assertEquals(listOf(0), result.selectedSubtitleTracks)
    }









}
