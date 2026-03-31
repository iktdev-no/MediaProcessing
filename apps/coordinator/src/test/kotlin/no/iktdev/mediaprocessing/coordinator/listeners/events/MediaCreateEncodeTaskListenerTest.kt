package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.*
import no.iktdev.mediaprocessing.defaultFilePrepareForWorkResultEvent
import no.iktdev.mediaprocessing.MockData.dummyAudioStream
import no.iktdev.mediaprocessing.MockData.dummyDisposition
import no.iktdev.mediaprocessing.MockData.dummyTags
import no.iktdev.mediaprocessing.MockData.dummyVideoStream
import no.iktdev.mediaprocessing.MockData.mediaParsedEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.LinearEncodeTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.SegmentedEncodeTask
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.common.model.task.data.DefaultEncodeData
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.MediaPreference
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class MediaCreateEncodeTaskListenerTest : TestBase() {

    private val listener = MediaCreateEncodeTaskListener(preference)

    @BeforeEach
    override fun setup() {
        mockkObject(TaskStore)
        every { TaskStore.persist(any()) } returns true

        every { preference.getMediaPreference() } returns MediaPreference(
            videoPreference = defaultVideoPreference,
            audioPreference = defaultAudioPreference
        )
    }

    // ------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------

    private fun List<String>.containsMapAudio(index: Int): Boolean =
        windowed(2).any { it[0] == "-map" && it[1] == "0:a:$index" }


    private fun assertAudioMapped(data: DefaultEncodeData, vararg indices: Int) {
        val audioArgs = data.audioInstructions.map { ffmpeg { fromInstructions(it) }.build() }
        indices.forEach { idx ->
            assertTrue(audioArgs.any { it.containsMapAudio(idx) }, "Expected audio map for index $idx")
        }
    }

    // ------------------------------------------------------------
    // SINGLE LANGUAGE (DEFAULT ONLY)
    // ------------------------------------------------------------

    @Test
    @DisplayName("""
    Når ett språk har default audio
    Hvis extended ikke finnes
    Så:
        Skal LinearEncodeTask brukes og inneholde ett audio-target
""")
    fun testSingleLanguageDefaultOnly() {
        var persistedTask: LinearEncodeTask? = null
        every { TaskStore.persist(any()) } answers {
            persistedTask = arg(0)
            true
        }

        val startEvent = StartProcessingEvent(
            StartData(setOf(OperationType.Encode), fileUri = "/tmp/movie.mkv")
        ).newReferenceId()
            .addToHistory()

        val parsed = mediaParsedEvent(
            "Baking Bread",
            "Baking Bread - S01E01 - Flour",
            MediaType.Serie
        ).derivedOf(startEvent)
            .addToHistory()

        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                videoStream = listOf(dummyVideoStream(index = 0, codec = "hevc", codec_tag_string = "[0][0][0][0]")),
                audioStream = listOf(
                    dummyAudioStream(
                        index = 1,
                        channels = 2,
                        disposition = dummyDisposition { default = true },
                        tags = dummyTags(language = "eng")
                    )
                )
            )
        ).derivedOf(parsed)
            .addToHistory()

        val preparedFile = defaultFilePrepareForWorkResultEvent()
            .derivedOf(parsedEvent)
            .addToHistory()

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
        ).derivedOf(preparedFile)
            .addToHistory()

        val result = listener.onEvent(selectedEvent, history)

        val task = persistedTask ?: fail("Task was not persisted")
        val data = task.data

        assertEquals("build/test-intermediate/Test.mkv", data.inputFile)
        assertEquals("Test.mp4", data.outputFileName)

        assertAudioMapped(data, 0)

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
            .addToHistory()

        val parsed = mediaParsedEvent("Baking Bread", "Baking Bread - S01E01 - Flour", MediaType.Serie)
            .derivedOf(startEvent)
            .addToHistory()

        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                videoStream = listOf(dummyVideoStream(index = 0, codec = "hevc", codec_tag_string = "[0][0][0][0]")),
                audioStream = listOf(
                    dummyAudioStream(index = 1, channels = 2, tags = dummyTags("eng")),
                    dummyAudioStream(index = 2, channels = 6, tags = dummyTags("eng"))
                )
            )
        ).derivedOf(parsed)
            .addToHistory()

        val preparedFile = defaultFilePrepareForWorkResultEvent()
            .derivedOf(parsedEvent)
            .addToHistory()


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
        ).derivedOf(preparedFile)
            .addToHistory()


        listener.onEvent(selectedEvent, history)

        val slot = slot<LinearEncodeTask>()
        verify { TaskStore.persist(capture(slot)) }

        assertAudioMapped(slot.captured.data, 0, 1)
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
        )
            .newReferenceId()
            .addToHistory()

        val parsed = mediaParsedEvent("Baking Bread", "Baking Bread - S01E01 - Flour", MediaType.Serie)
            .derivedOf(startEvent)
            .addToHistory()

        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                videoStream = listOf(dummyVideoStream(index = 0, codec = "hevc", codec_tag_string = "[0][0][0][0]")),
                audioStream = listOf(
                    dummyAudioStream(index = 1, channels = 2, tags = dummyTags("eng")),
                    dummyAudioStream(index = 2, channels = 6, tags = dummyTags("eng")),
                    dummyAudioStream(index = 3, channels = 2, tags = dummyTags("jpn")),
                    dummyAudioStream(index = 4, channels = 6, tags = dummyTags("jpn"))
                )
            )
        )
            .derivedOf(parsed)
            .addToHistory()

        val preparedFile = defaultFilePrepareForWorkResultEvent()
            .derivedOf(parsedEvent)
            .addToHistory()

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
        ).derivedOf(preparedFile)
            .addToHistory()


        listener.onEvent(selectedEvent, history)

        val slot = slot<LinearEncodeTask>()
        verify { TaskStore.persist(capture(slot)) }

        assertAudioMapped(slot.captured.data, 0, 1, 2, 3)

    }

    @Test
    @DisplayName("""
    Når video må reencodes
    Hvis codec ikke matcher preferanse
    Så:
        Skal SegmentedEncodeTask brukes
""")
    fun testSegmentedSingleLanguage() {
        var persistedTask: SegmentedEncodeTask? = null
        every { TaskStore.persist(any()) } answers {
            persistedTask = arg(0)
            true
        }

        val startEvent = StartProcessingEvent(
            StartData(setOf(OperationType.Encode), fileUri = "/tmp/movie.mkv")
        ).newReferenceId()
            .addToHistory()

        val parsed = mediaParsedEvent(
            "Baking Bread",
            "Baking Bread - S01E01 - Flour",
            MediaType.Serie
        ).derivedOf(startEvent)
            .addToHistory()

        // IMPORTANT: codec = "h264" → forces Reencode
        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                videoStream = listOf(dummyVideoStream(index = 0, codec = "h264", codec_tag_string = "avc1")),
                audioStream = listOf(
                    dummyAudioStream(
                        index = 1,
                        channels = 2,
                        disposition = dummyDisposition { default = true },
                        tags = dummyTags(language = "eng")
                    )
                )
            )
        ).derivedOf(parsed)
            .addToHistory()

        val preparedFile = defaultFilePrepareForWorkResultEvent()
            .derivedOf(parsedEvent)
            .addToHistory()

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
        ).derivedOf(preparedFile)
            .addToHistory()

        val result = listener.onEvent(selectedEvent, history)

        val task = persistedTask ?: fail("SegmentedEncodeTask was not persisted")
        val data = (task as? SegmentedEncodeTask)!!.data

        assertEquals("build/test-intermediate/Test.mkv", data.inputFile)
        assertEquals("Test.mp4", data.outputFileName)


        // Segmented has separate video/audio args
        assertNotNull(data.videoInstruction)
        assertTrue(data.audioInstructions.isNotEmpty())

        assertTrue(result is ProcesserEncodeTaskCreatedEvent)
    }

    @Test
    @DisplayName("""
    Når video må reencodes
    Hvis default + extended audio er valgt
    Så:
        Skal SegmentedEncodeTask inneholde begge audio-targets
""")
    fun testSegmentedWithExtendedAudio() {
        var persistedTask: SegmentedEncodeTask? = null
        every { TaskStore.persist(any()) } answers {
            persistedTask = arg(0)
            true
        }

        val startEvent = StartProcessingEvent(
            StartData(setOf(OperationType.Encode), fileUri = "/tmp/movie.mkv")
        ).newReferenceId()
            .addToHistory()

        val parsed = mediaParsedEvent(
            "Baking Bread",
            "Baking Bread - S01E01 - Flour",
            MediaType.Serie
        ).derivedOf(startEvent)
            .addToHistory()

        // Force Reencode by using h264 instead of hevc
        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                videoStream = listOf(dummyVideoStream(index = 0, codec = "h264", codec_tag_string = "avc1")),
                audioStream = listOf(
                    dummyAudioStream(index = 1, channels = 2, tags = dummyTags("eng")),
                    dummyAudioStream(index = 2, channels = 6, tags = dummyTags("eng"))
                )
            )
        ).derivedOf(parsed)
            .addToHistory()

        val preparedFile = defaultFilePrepareForWorkResultEvent()
            .derivedOf(parsedEvent)
            .addToHistory()

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
        ).derivedOf(preparedFile)
            .addToHistory()

        listener.onEvent(selectedEvent, history)

        val task = persistedTask ?: fail("SegmentedEncodeTask was not persisted")
        val data = task.data

        // Two audio argument lists
        assertEquals(2, data.audioInstructions.size)
    }
}
