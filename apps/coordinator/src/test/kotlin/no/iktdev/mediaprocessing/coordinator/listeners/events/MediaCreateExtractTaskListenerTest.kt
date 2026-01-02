package no.iktdev.mediaprocessing.coordinator.listeners.events


import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.verify
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleTags
import no.iktdev.mediaprocessing.ffmpeg.data.Tags
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksExtractSelectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import no.iktdev.mediaprocessing.shared.common.stores.TaskStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import java.util.UUID

class MediaCreateExtractTaskListenerTest {

    object FakeTaskStore: no.iktdev.eventi.stores.TaskStore {
        val persisted = mutableListOf<Task>()
        override fun persist(task: Task) {
            persisted.add(task)
        }

        override fun findByTaskId(taskId: UUID): PersistedTask? { TODO("Not yet implemented") }
        override fun findByReferenceId(referenceId: UUID): List<PersistedTask> { TODO("Not yet implemented") }
        override fun findUnclaimed(referenceId: UUID): List<PersistedTask> { TODO("Not yet implemented") }
        override fun claim(taskId: UUID, workerId: String): Boolean { TODO("Not yet implemented") }
        override fun heartbeat(taskId: UUID) { TODO("Not yet implemented") }
        override fun markConsumed(taskId: UUID, status: TaskStatus) { TODO("Not yet implemented") }
        override fun releaseExpiredTasks(timeout: Duration) { TODO("Not yet implemented") }
        override fun getPendingTasks(): List<PersistedTask> { TODO("Not yet implemented") }
    }

    @BeforeEach
    fun setup() {
        mockkObject(TaskStore)
        every { TaskStore.persist(any()) } just Runs
    }

    private val listener = MediaCreateExtractTaskListener()

    private fun dummyStream(
        index: Int,
        codecName: String,
        language: String? = null
    ): SubtitleStream {
        return SubtitleStream(
            index = index,
            codec_name = codecName,
            codec_long_name = codecName,
            codec_type = "subtitle",
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
                title = null,
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
            ),
            subtitle_tags = SubtitleTags(
                language = language,
                filename = null,
                mimetype = null
            )
        )
    }

    @Test
    @DisplayName("""
        Hvis en SRT-subtitle med språk er valgt
        Når toSubtitleArgumentData kalles
        Så:
            Returneres et ExtractSubtitleData-objekt
            Outputfilen får .srt-extension og språk i navnet
            Argumentlisten inneholder -map og -c:s copy
    """)
    fun testSrtSubtitle() {
        val stream = dummyStream(0, "subrip", "eng")
        val inputFile = File("/tmp/movie.mkv")

        val result = listener.toSubtitleArgumentData(0, inputFile, stream)

        assertNotNull(result)
        assertEquals("movie-eng.srt", result!!.outputFileName)
        assertEquals("eng", result.language)
        assertEquals(listOf("-map", "0:s:0", "-c:s", "copy"), result.arguments)
    }

    @Test
    @DisplayName("""
        Hvis codec ikke støttes (f.eks pgssub)
        Når toSubtitleArgumentData kalles
        Så:
            Returneres null
            Ingen ExtractSubtitleData opprettes
    """)
    fun testUnsupportedCodec() {
        val stream = dummyStream(1, "pgssub", "eng")
        val inputFile = File("/tmp/movie.mkv")

        val result = listener.toSubtitleArgumentData(1, inputFile, stream)

        assertNull(result)
    }

    @Test
    @DisplayName("""
        Hvis språk mangler i subtitle-stream
        Når toSubtitleArgumentData kalles
        Så:
            Returneres null
            Ingen ExtractSubtitleData opprettes
    """)
    fun testMissingLanguage() {
        val stream = dummyStream(2, "subrip", null)
        val inputFile = File("/tmp/movie.mkv")

        val result = listener.toSubtitleArgumentData(2, inputFile, stream)

        assertNull(result)
    }

    @Test
    @DisplayName("""
        Hvis en ASS-subtitle med språk er valgt
        Når toSubtitleArgumentData kalles
        Så:
            Returneres et ExtractSubtitleData-objekt
            Outputfilen får .ass-extension og språk i navnet
            Argumentlisten inneholder -map og -c:s copy
    """)
    fun testAssSubtitle() {
        val stream = dummyStream(3, "ass", "jpn")
        val inputFile = File("/tmp/anime.mkv")

        val result = listener.toSubtitleArgumentData(3, inputFile, stream)

        assertNotNull(result)
        assertEquals("anime-jpn.ass", result!!.outputFileName)
        assertEquals("jpn", result.language)
        assertEquals(listOf("-map", "0:s:3", "-c:s", "copy"), result.arguments)
    }

    @Test
    @DisplayName("""
        Hvis en StartProcessingEvent og MediaStreamParsedEvent finnes i historikken
        Når onEvent kalles med MediaTracksExtractSelectedEvent som velger en SRT-subtitle
        Så:
            Returneres et ProcesserExtractTaskCreatedEvent
            tasksCreated-listen inneholder minst én UUID
    """)
    fun testOnEventCreatesTasks() {
        val startEvent = StartProcessingEvent(
            StartData(setOf(OperationType.Extract), fileUri = "/tmp/movie.mkv")
        )
        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(subtitleStream = listOf(dummyStream(0, "subrip", "eng")))
        )
        val selectedEvent = MediaTracksExtractSelectedEvent(selectedSubtitleTracks = listOf(0))

        val history = listOf(startEvent, parsedEvent)

        val result = listener.onEvent(selectedEvent, history)

        assertNotNull(result)
        assertTrue(result is ProcesserExtractTaskCreatedEvent)
        val created = result as ProcesserExtractTaskCreatedEvent
        assertTrue(created.tasksCreated.isNotEmpty())
        verify {
            TaskStore.persist(withArg { task ->
                assertTrue(task is ExtractSubtitleTask)
                val data = (task as ExtractSubtitleTask).data
                assertEquals("/tmp/movie.mkv", data.inputFile)
                assertEquals("movie-eng.srt", data.outputFileName)
                assertEquals("eng", data.language)
                assertEquals(listOf("-map", "0:s:0", "-c:s", "copy"), data.arguments)
            })
        }
    }

    @Test
    @DisplayName("""
        Hvis flere undertekster (SRT og ASS) er valgt
        Når onEvent kalles
        Så:
            TaskStore.persist skal kalles én gang per valgt spor
            Hvert ExtractSubtitleTask skal ha korrekt data (filnavn, språk, arguments)
    """)
    fun testOnEventWithMultipleSubtitles() {
        // Hvis: vi har en StartProcessingEvent og to subtitle streams
        val startEvent = StartProcessingEvent(
            StartData(setOf(OperationType.Extract), fileUri = "/tmp/movie.mkv")
        )
        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                subtitleStream = listOf(
                    dummyStream(0, "subrip", "eng"),
                    dummyStream(1, "ass", "jpn")
                )
            )
        )
        val selectedEvent = MediaTracksExtractSelectedEvent(selectedSubtitleTracks = listOf(0, 1))

        val history = listOf(startEvent, parsedEvent)

        // Når: vi kaller onEvent
        val result = listener.onEvent(selectedEvent, history)

        // Så: TaskStore.persist skal ha blitt kalt to ganger
        verify(exactly = 2) { TaskStore.persist(any()) }

        // Fang begge objektene
        val slot = mutableListOf<Task>()
        verify { TaskStore.persist(capture(slot)) }

        // Sjekk første (SRT)
        val srtTask = slot[0] as ExtractSubtitleTask
        assertEquals("movie-eng.srt", srtTask.data.outputFileName)
        assertEquals("eng", srtTask.data.language)
        assertEquals(listOf("-map", "0:s:0", "-c:s", "copy"), srtTask.data.arguments)

        // Sjekk andre (ASS)
        val assTask = slot[1] as ExtractSubtitleTask
        assertEquals("movie-jpn.ass", assTask.data.outputFileName)
        assertEquals("jpn", assTask.data.language)
        assertEquals(listOf("-map", "0:s:1", "-c:s", "copy"), assTask.data.arguments)

        // Og: resultatet er et ProcesserExtractTaskCreatedEvent med to taskIds
        assertTrue(result is ProcesserExtractTaskCreatedEvent)
        val created = result as ProcesserExtractTaskCreatedEvent
        assertEquals(2, created.tasksCreated.size)
    }
}
