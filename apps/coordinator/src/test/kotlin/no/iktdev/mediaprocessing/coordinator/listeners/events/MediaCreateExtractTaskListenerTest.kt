package no.iktdev.mediaprocessing.coordinator.listeners.events


import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import no.iktdev.eventi.models.StoreResult
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.MockData.mediaParsedEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.defaultFilePrepareForWorkResultEvent
import no.iktdev.mediaprocessing.ffmpeg.assertContainsAllWithOffset
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.ffmpeg.data.Tags
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class MediaCreateExtractTaskListenerTest: TestBase() {

    object FakeTaskStore: no.iktdev.eventi.stores.TaskStore {
        val persisted = mutableListOf<Task>()
        override fun persist(task: Task): Boolean {
            persisted.add(task)
            return true
        }

        override fun findByTaskId(taskId: UUID): PersistedTask? { TODO("Not yet implemented") }
        override fun findByReferenceId(referenceId: UUID): List<PersistedTask> { TODO("Not yet implemented") }
        override fun findUnclaimed(referenceId: UUID): List<PersistedTask> { TODO("Not yet implemented") }
        override fun claim(taskId: UUID, workerId: String): Boolean { TODO("Not yet implemented") }
        override fun heartbeat(taskId: UUID): Boolean { TODO("Not yet implemented") }
        override fun markConsumed(taskId: UUID, status: TaskStatus): Boolean { TODO("Not yet implemented") }
        override fun releaseExpiredTasks() { TODO("Not yet implemented") }
        override fun resetTaskById(taskId: UUID): Boolean {
            TODO("Not yet implemented")
        }

        override fun resetTasksById(taskId: List<UUID>): StoreResult {
            TODO("Not yet implemented")
        }

        override fun deleteTasksById(taskId: UUID): StoreResult {
            TODO("Not yet implemented")
        }

        override fun getPendingTasks(): List<PersistedTask> { TODO("Not yet implemented") }
    }

    @BeforeEach
    override fun setup() {
        mockkObject(TaskStore)
        every { TaskStore.persist(any()) } returns true
        super.setup()
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
        val inputFile = IFile("/tmp/movie.mkv")

        val result = listener.toSubtitleArgumentData(0, inputFile, "subby", stream)

        assertNotNull(result)
        assertEquals("movie-eng.srt", result!!.outputFileName)
        assertEquals("eng", result.language)
        val args = ffmpeg { fromInstructions(result.instructions) }.build()
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1", "-map", "0:s:0", "-c:s:0", "copy"),
            args, 7, 1
        )
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
        val inputFile = IFile("/tmp/movie.mkv")

        val result = listener.toSubtitleArgumentData(1, inputFile, "subby",stream)

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
        val inputFile = IFile("/tmp/movie.mkv")

        val result = listener.toSubtitleArgumentData(2, inputFile, "subby", stream)

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
        val inputFile = IFile("/tmp/anime.mkv")

        val result = listener.toSubtitleArgumentData(3, inputFile, "subby", stream)

        assertNotNull(result)
        assertEquals("anime-jpn.ass", result!!.outputFileName)
        assertEquals("jpn", result.language)
        val args = ffmpeg { fromInstructions(result.instructions) }.build()
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1", "-map", "0:s:3", "-c:s:0", "copy"),
            args, 7, 1)
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
            StartData(setOf(OperationType.ExtractSubtitles), fileUri = "/tmp/movie.mkv")
        ).newReferenceId()
            .addToHistory()

        val parsed = mediaParsedEvent("Baking Bread", "Baking Bread - S01E01 - Flour", MediaType.Serie)
            .derivedOf(startEvent)
            .addToHistory()

        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(subtitleStream = listOf(dummyStream(0, "subrip", "eng")))
        ).derivedOf(parsed)
            .addToHistory()

        val preparedFile = defaultFilePrepareForWorkResultEvent()
            .derivedOf(parsedEvent)
            .addToHistory()

        val selectedEvent = MediaTracksExtractSelectedEvent(selectedSubtitleTracks = listOf(0))
            .derivedOf(preparedFile)
            .addToHistory()


        val result = listener.onEvent(selectedEvent, history)

        val slot = slot<ExtractSubtitleTask>()
        verify { TaskStore.persist(capture(slot)) }

        val data = slot.captured.data

        val created = result as ProcesserExtractTaskCreatedEvent
        assertTrue(created.taskIds.isNotEmpty())
        assertEquals("build/test-intermediate/Test.mkv", data.inputFile)
        assertEquals("Test-eng.srt", data.outputFileName)
        assertEquals("eng", data.language)
        val args = ffmpeg { fromInstructions(data.instructions) }.build()
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1", "-map", "0:s:0", "-c:s:0", "copy"), args, 7, 1)
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
            StartData(setOf(OperationType.ExtractSubtitles), fileUri = "/tmp/movie.mkv")
        ).newReferenceId()
            .addToHistory()

        val parsed = mediaParsedEvent("Baking Bread", "Baking Bread - S01E01 - Flour", MediaType.Serie)
            .derivedOf(startEvent)
            .addToHistory()

        val parsedEvent = MediaStreamParsedEvent(
            data = ParsedMediaStreams(
                subtitleStream = listOf(
                    dummyStream(0, "subrip", "eng"),
                    dummyStream(1, "ass", "jpn")
                )
            )
        ).derivedOf(parsed)
            .addToHistory()

        val preparedFile = defaultFilePrepareForWorkResultEvent()
            .derivedOf(parsedEvent)
            .addToHistory()

        val selectedEvent = MediaTracksExtractSelectedEvent(selectedSubtitleTracks = listOf(0, 1))
            .derivedOf(preparedFile)


        // Når: vi kaller onEvent
        val result = listener.onEvent(selectedEvent, history)

        // Så: TaskStore.persist skal ha blitt kalt to ganger
        verify(exactly = 2) { TaskStore.persist(any()) }

        // Fang begge objektene
        val slot = mutableListOf<Task>()
        verify { TaskStore.persist(capture(slot)) }

        // Sjekk første (SRT)
        val srtTask = slot[0] as ExtractSubtitleTask
        assertEquals("Test-eng.srt", srtTask.data.outputFileName)
        assertEquals("eng", srtTask.data.language)
        val args1 = srtTask.data.instructions.let { ffmpeg { fromInstructions(it) }.build() }
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1", "-map", "0:s:0", "-c:s:0", "copy"), args1, 7, 1)

        // Sjekk andre (ASS)
        val assTask = slot[1] as ExtractSubtitleTask
        assertEquals("Test-jpn.ass", assTask.data.outputFileName)
        assertEquals("jpn", assTask.data.language)
        val args2 = assTask.data.instructions.let { ffmpeg { fromInstructions(it) }.build() }
        assertContainsAllWithOffset(listOf("-map_chapters", "-1", "-map", "0:s:1", "-c:s:0", "copy"), args2, 7, 1)

        // Og: resultatet er et ProcesserExtractTaskCreatedEvent med to taskIds
        assertTrue(result is ProcesserExtractTaskCreatedEvent)
        val created = result as ProcesserExtractTaskCreatedEvent
        assertEquals(2, created.taskIds.size)
    }
}
