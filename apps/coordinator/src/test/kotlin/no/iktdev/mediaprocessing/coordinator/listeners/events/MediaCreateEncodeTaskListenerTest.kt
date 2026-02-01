package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.*
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.coordinator.AudioPreference
import no.iktdev.mediaprocessing.coordinator.ProcesserPreference
import no.iktdev.mediaprocessing.coordinator.VideoPreference
import no.iktdev.mediaprocessing.ffmpeg.data.*
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MediaCreateEncodeTaskListenerTest: TestBase() {

    private val listener = MediaCreateEncodeTaskListener(preference)

    @BeforeEach
    override fun setup() {
        mockkObject(TaskStore)
        every { TaskStore.persist(any()) } just Runs
        every { preference.getProcesserPreference() } returns ProcesserPreference(
            videoPreference = VideoPreference(codec = VideoCodec.Hevc()),
            audioPreference = AudioPreference(codec = AudioCodec.Aac(channels = 2))
        )
    }



    @Test
    @DisplayName("""
        Hvis en video- og audio-track er valgt
        Når onEvent kalles
        Så:
            TaskStore.persist mottar et EncodeTask
            data-feltet har korrekt inputFile, outputFileName og arguments fra MediaPlan
    """)
    fun testOnEventWithSingleAudioTrack() {
        val startEvent = StartProcessingEvent(
            StartData(setOf(OperationType.Encode), fileUri = "/tmp/movie.mkv")
        ).newReferenceId()
        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                videoStream = listOf(mockVideoStream(index = 0, codec = "h264", disposition = mockDisposition(), tags = mockTags())),
                audioStream = listOf(mockAudioStream(index = 1, codec = "aac", disposition = mockDisposition(), tags = mockTags()))
            )
        ).derivedOf(startEvent)
        val selectedEvent = MediaTracksEncodeSelectedEvent(
            selectedVideoTrack = 0,
            selectedAudioTrack = 0
        ).derivedOf(parsedEvent)

        val history = listOf(startEvent, parsedEvent)

        val result = listener.onEvent(selectedEvent, history)

        verify {
            TaskStore.persist(withArg { task ->
                assertTrue(task is EncodeTask)
                val data = (task as EncodeTask).data
                assertEquals("/tmp/movie.mkv", data.inputFile)
                assertEquals("movie.mp4", data.outputFileName)
                assertTrue(data.arguments.isNotEmpty(), "Arguments from MediaPlan should not be empty")
            })
        }

        assertTrue(result is ProcesserEncodeTaskCreatedEvent)
    }

    @Test
    @DisplayName("""
        Hvis en video- og to audio-tracks (inkludert extended) er valgt
        Når onEvent kalles
        Så:
            TaskStore.persist mottar et EncodeTask
            data-feltet inkluderer begge audio-targets i arguments
    """)
    fun testOnEventWithExtendedAudioTrack() {
        val startEvent = StartProcessingEvent(
            StartData(setOf(OperationType.Encode), fileUri = "/tmp/movie.mkv")
        ).newReferenceId()

        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                videoStream = listOf(mockVideoStream(index = 0, codec = "h264", disposition = mockDisposition(), tags = mockTags())),
                audioStream = listOf(
                    mockAudioStream(index = 1, codec = "aac", disposition = mockDisposition(), tags = mockTags()),
                    mockAudioStream(index = 2, codec = "aac", disposition = mockDisposition(), tags = mockTags())
                )
            )
        ).derivedOf(startEvent)
        val selectedEvent = MediaTracksEncodeSelectedEvent(
            selectedVideoTrack = 0,
            selectedAudioTrack = 0,
            selectedAudioExtendedTrack = 1
        ).derivedOf(parsedEvent)

        val history = listOf(startEvent, parsedEvent)

        val result = listener.onEvent(selectedEvent, history)

        verify {
            TaskStore.persist(withArg { task ->
                val data = (task as EncodeTask).data
                assertEquals("/tmp/movie.mkv", data.inputFile)
                assertEquals("movie.mp4", data.outputFileName)
                // her kan du sjekke at begge audio-tracks er med i ffmpeg-args
                assertTrue(data.arguments.any { it.contains("0:a:0") })
                assertTrue(data.arguments.any { it.contains("0:a:1") })
            })
        }

        assertTrue(result is ProcesserEncodeTaskCreatedEvent)
    }

    // Dummy streams for test
    fun mockVideoStream(
        index: Int = 0,
        codec: String = "h264",
        width: Int = 1920,
        height: Int = 1080,
        disposition: Disposition,
        tags: Tags
    ) = VideoStream(
        index = index,
        codec_name = codec,
        codec_long_name = "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10",
        codec_type = "video",
        codec_tag_string = "avc1",
        codec_tag = "0x31637661",
        r_frame_rate = "25/1",
        avg_frame_rate = "25/1",
        time_base = "1/90000",
        start_pts = 0,
        start_time = "0.000000",
        disposition = disposition,
        tags = tags,
        duration = "60.0",
        duration_ts = 54000,
        profile = "High",
        width = width,
        height = height,
        coded_width = width,
        coded_height = height,
        closed_captions = 0,
        has_b_frames = 2,
        sample_aspect_ratio = "1:1",
        display_aspect_ratio = "16:9",
        pix_fmt = "yuv420p",
        level = 40,
        color_range = "tv",
        color_space = "bt709",
        color_transfer = "bt709",
        color_primaries = "bt709",
        chroma_location = "left",
        refs = 1
    )

    fun mockAudioStream(
        index: Int = 0,
        codec: String = "aac",
        channels: Int = 2,
        profile: String = "LC",
        disposition: Disposition,
        tags: Tags
    ) = AudioStream(
        index = index,
        codec_name = codec,
        codec_long_name = "AAC (Advanced Audio Coding)",
        codec_type = "audio",
        codec_tag_string = "mp4a",
        codec_tag = "0x6134706d",
        r_frame_rate = "0/0",
        avg_frame_rate = "0/0",
        time_base = "1/48000",
        start_pts = 0,
        start_time = "0.000000",
        duration = "60.0",
        duration_ts = 2880000,
        disposition = disposition,
        tags = tags,
        profile = profile,
        sample_fmt = "fltp",
        sample_rate = "48000",
        channels = channels,
        channel_layout = "stereo",
        bits_per_sample = 0
    )

    fun mockDisposition(
        default: Int = 1,
        forced: Int = 0
    ) = Disposition(
        default = default,
        dub = 0,
        original = 0,
        comment = 0,
        lyrics = 0,
        karaoke = 0,
        forced = forced,
        hearing_impaired = 0,
        captions = 0,
        visual_impaired = 0,
        clean_effects = 0,
        attached_pic = 0,
        timed_thumbnails = 0
    )

    fun mockTags(
        language: String? = "eng",
        title: String? = null,
        filename: String? = null
    ) = Tags(
        title = title,
        BPS = null,
        DURATION = null,
        NUMBER_OF_FRAMES = 0,
        NUMBER_OF_BYTES = null,
        _STATISTICS_WRITING_APP = null,
        _STATISTICS_WRITING_DATE_UTC = null,
        _STATISTICS_TAGS = null,
        language = language,
        filename = filename,
        mimetype = null
    )
}