package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.*
import no.iktdev.mediaprocessing.MockData.dummyAudioStream
import no.iktdev.mediaprocessing.MockData.dummyDisposition
import no.iktdev.mediaprocessing.MockData.dummyTags
import no.iktdev.mediaprocessing.MockData.dummyVideoStream
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.ffmpeg.data.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.ProcesserPreference
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MediaCreateEncodeTaskListenerTest : TestBase() {

    private val listener = MediaCreateEncodeTaskListener(preference)

    @BeforeEach
    override fun setup() {
        mockkObject(TaskStore)
        every { TaskStore.persist(any()) } just Runs

        every { preference.getProcesserPreference() } returns ProcesserPreference(
            videoPreference = defaultVideoPreference,
            audioPreference = defaultAudioPreference
        )
    }

    // ------------------------------------------------------------
    // SINGLE LANGUAGE (DEFAULT ONLY)
    // ------------------------------------------------------------

    @Test
    @DisplayName("""
        Når ett språk har default audio
        Hvis extended ikke finnes
        Så:
            Skal EncodeTask inneholde ett audio-target
    """)
    fun testSingleLanguageDefaultOnly() {
        val startEvent = StartProcessingEvent(
            StartData(setOf(OperationType.Encode), fileUri = "/tmp/movie.mkv")
        ).newReferenceId()

        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                videoStream = listOf(dummyVideoStream(index = 0)),
                audioStream = listOf(
                    dummyAudioStream(
                        index = 1,
                        channels = 2,
                        disposition = dummyDisposition { default = true },
                        tags = dummyTags(language = "eng")
                    )
                )
            )
        ).derivedOf(startEvent)

        val selectedEvent = MediaTracksEncodeSelectedEvent(
            selectedVideoTrack = 0,
            audioTracks = listOf(
                MediaTracksEncodeSelectedEvent.SelectedAudioTracks(
                    language = "eng",
                    defaultListIndex = 0,
                    defaultFfmpegIndex = 1,
                    extendedListIndex = null,
                    extendedFfmpegIndex = null
                )
            )
        ).derivedOf(parsedEvent)

        val history = listOf(startEvent, parsedEvent)

        val result = listener.onEvent(selectedEvent, history)

        val slot = slot<EncodeTask>()
        verify { TaskStore.persist(capture(slot)) }

        val data = slot.captured.data

        assertEquals("/tmp/movie.mkv", data.inputFile)
        assertEquals("movie.mp4", data.outputFileName)
        assertTrue(data.arguments.containsMapAudio(1))

        assertTrue(result is ProcesserEncodeTaskCreatedEvent)
    }

    // ------------------------------------------------------------
    // SINGLE LANGUAGE WITH EXTENDED
    // ------------------------------------------------------------

    @Test
    @DisplayName("""
        Når ett språk har både default og extended
        Hvis begge er valgt
        Så:
            Skal EncodeTask inneholde to audio-targets
    """)
    fun testSingleLanguageWithExtended() {
        val startEvent = StartProcessingEvent(
            StartData(setOf(OperationType.Encode), fileUri = "/tmp/movie.mkv")
        ).newReferenceId()

        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                videoStream = listOf(dummyVideoStream(index = 0)),
                audioStream = listOf(
                    dummyAudioStream(index = 1, channels = 2, tags = dummyTags("eng")),
                    dummyAudioStream(index = 2, channels = 6, tags = dummyTags("eng"))
                )
            )
        ).derivedOf(startEvent)

        val selectedEvent = MediaTracksEncodeSelectedEvent(
            selectedVideoTrack = 0,
            audioTracks = listOf(
                MediaTracksEncodeSelectedEvent.SelectedAudioTracks(
                    language = "eng",
                    defaultListIndex = 0,
                    defaultFfmpegIndex = 1,
                    extendedListIndex = 1,
                    extendedFfmpegIndex = 2
                )
            )
        ).derivedOf(parsedEvent)

        val history = listOf(startEvent, parsedEvent)

        listener.onEvent(selectedEvent, history)

        val slot = slot<EncodeTask>()
        verify { TaskStore.persist(capture(slot)) }

        val args = slot.captured.data.arguments

        assertTrue(args.containsMapAudio(1))
        assertTrue(args.containsMapAudio(2))
    }

    // ------------------------------------------------------------
    // MULTI-LANGUAGE DEFAULT + EXTENDED
    // ------------------------------------------------------------

    @Test
    @DisplayName("""
        Når flere språk er valgt
        Hvis hvert språk har default og extended
        Så:
            Skal EncodeTask inneholde audio-targets for alle språk
    """)
    fun testMultiLanguageDefaultAndExtended() {
        val startEvent = StartProcessingEvent(
            StartData(setOf(OperationType.Encode), fileUri = "/tmp/movie.mkv")
        ).newReferenceId()

        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                videoStream = listOf(dummyVideoStream(index = 0)),
                audioStream = listOf(
                    dummyAudioStream(index = 1, channels = 2, tags = dummyTags("eng")),
                    dummyAudioStream(index = 2, channels = 6, tags = dummyTags("eng")),
                    dummyAudioStream(index = 3, channels = 2, tags = dummyTags("jpn")),
                    dummyAudioStream(index = 4, channels = 6, tags = dummyTags("jpn"))
                )
            )
        ).derivedOf(startEvent)

        val selectedEvent = MediaTracksEncodeSelectedEvent(
            selectedVideoTrack = 0,
            audioTracks = listOf(
                MediaTracksEncodeSelectedEvent.SelectedAudioTracks(
                    language = "eng",
                    defaultListIndex = 0,
                    defaultFfmpegIndex = 1,
                    extendedListIndex = 1,
                    extendedFfmpegIndex = 2
                ),
                MediaTracksEncodeSelectedEvent.SelectedAudioTracks(
                    language = "jpn",
                    defaultListIndex = 2,
                    defaultFfmpegIndex = 3,
                    extendedListIndex = 3,
                    extendedFfmpegIndex = 4
                )
            )
        ).derivedOf(parsedEvent)

        val history = listOf(startEvent, parsedEvent)

        listener.onEvent(selectedEvent, history)

        val slot = slot<EncodeTask>()
        verify { TaskStore.persist(capture(slot)) }

        val args = slot.captured.data.arguments

        assertTrue(args.containsMapAudio(1))
        assertTrue(args.containsMapAudio(2))
        assertTrue(args.containsMapAudio(3))
        assertTrue(args.containsMapAudio(4))
    }

    // ------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------

    private fun List<String>.containsMapAudio(index: Int): Boolean =
        windowed(2).any { it[0] == "-map" && it[1] == "0:a:$index" }
}
