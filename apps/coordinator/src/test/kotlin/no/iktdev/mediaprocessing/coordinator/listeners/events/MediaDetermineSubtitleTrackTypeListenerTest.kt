@file:Suppress("JUnitMalformedDeclaration")

package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.ffmpeg.data.Tags
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksDetermineSubtitleTypeEvent
import no.iktdev.mediaprocessing.shared.common.model.SubtitleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class MediaDetermineSubtitleTrackTypeListenerTest {

    private val listener = MediaDetermineSubtitleTrackTypeListener()



    data class SubtitleTestCase(
        val stream: SubtitleStream,
        val expectedType: SubtitleType,
        val expectedKept: Boolean
    )

    companion object {

        private fun makeStream(codec: String, title: String?, language: String = "eng"): SubtitleStream {
            return SubtitleStream(
                index = 0,
                codec_name = codec,
                codec_long_name = codec,
                codec_type = codec, // NB: her brukes codec_type i onlySupportedCodecs
                codec_tag_string = "",
                codec_tag = "",
                r_frame_rate = "0/0",
                avg_frame_rate = "0/0",
                time_base = "1/1000",
                start_pts = 0,
                start_time = "0",
                duration = null,
                duration_ts = null,
                disposition = null,
                tags = Tags(
                    title = title,
                    BPS = null,
                    DURATION = null,
                    NUMBER_OF_FRAMES = 0,
                    NUMBER_OF_BYTES = null,
                    _STATISTICS_WRITING_APP = null,
                    _STATISTICS_WRITING_DATE_UTC = null,
                    _STATISTICS_TAGS = null,
                    language = language,
                    filename = null,
                    mimetype = null
                )
            )
        }

        @JvmStatic
        fun subtitleCases(): Stream<Named<SubtitleTestCase>> {
            return Stream.of(
                Named.of("Commentary filtered out",
                    SubtitleTestCase(
                        stream = makeStream("ass", "Director Commentary"),
                        expectedType = SubtitleType.Commentary,
                        expectedKept = false
                    )
                ),
                Named.of("Song filtered out",
                    SubtitleTestCase(
                        stream = makeStream("subrip", "Song Lyrics"),
                        expectedType = SubtitleType.Song,
                        expectedKept = false
                    )
                ),
                Named.of("Closed Caption filtered out",
                    SubtitleTestCase(
                        stream = makeStream("webvtt", "Closed Caption"),
                        expectedType = SubtitleType.ClosedCaption,
                        expectedKept = false
                    )
                ),
                Named.of("SHD filtered out",
                    SubtitleTestCase(
                        stream = makeStream("smi", "SHD"),
                        expectedType = SubtitleType.SHD,
                        expectedKept = false
                    )
                ),
                Named.of("Dialogue kept",
                    SubtitleTestCase(
                        stream = makeStream("ass", "Normal Dialogue"),
                        expectedType = SubtitleType.Dialogue,
                        expectedKept = true
                    )
                ),
                Named.of("Unsupported codec filtered out",
                    SubtitleTestCase(
                        stream = makeStream("pgssub", "Dialogue"),
                        expectedType = SubtitleType.Dialogue,
                        expectedKept = false
                    )
                ),
                Named.of("Commentary with typo",
                    SubtitleTestCase(
                        stream = makeStream("ass", "Comentary track"), // missing 'm'
                        expectedType = SubtitleType.Commentary,
                        expectedKept = false
                    )
                ),
                Named.of("Song with variant spelling",
                    SubtitleTestCase(
                        stream = makeStream("subrip", "Sogn lyrics"), // 'song' misspelled
                        expectedType = SubtitleType.Song,
                        expectedKept = false
                    )
                ),
                Named.of("Closed Caption with dash",
                    SubtitleTestCase(
                        stream = makeStream("webvtt", "Closed-caption subs"),
                        expectedType = SubtitleType.ClosedCaption,
                        expectedKept = false
                    )
                ),
                Named.of("SHD with abbreviation",
                    SubtitleTestCase(
                        stream = makeStream("smi", "HH subs"), // 'hh' is in SHD filters
                        expectedType = SubtitleType.SHD,
                        expectedKept = false
                    )
                ),
                Named.of("Dialogue with extra tags",
                    SubtitleTestCase(
                        stream = makeStream("ass", "Dialogue [ENG] normal"),
                        expectedType = SubtitleType.Dialogue,
                        expectedKept = true
                    )
                ),
                Named.of("Unsupported codec with random title",
                    SubtitleTestCase(
                        stream = makeStream("pgssub", "Commentary track"),
                        expectedType = SubtitleType.Commentary,
                        expectedKept = false
                    )
                )
            )
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("subtitleCases")
    @DisplayName("Hvis ulike subtitles testes → riktig type og filtrering")
    fun testSubtitleCases(testCase: SubtitleTestCase) {
        val event = MediaStreamParsedEvent(
            ParsedMediaStreams(subtitleStream = listOf(testCase.stream))
        ).newReferenceId()
        val result = listener.onEvent(event, emptyList()) as MediaTracksDetermineSubtitleTypeEvent

        if (testCase.expectedKept) {
            assertEquals(1, result.subtitleTrackItems.size)
            assertEquals(testCase.expectedType, result.subtitleTrackItems[0].type)
        } else {
            assertTrue(result.subtitleTrackItems.isEmpty())
        }
    }
}
