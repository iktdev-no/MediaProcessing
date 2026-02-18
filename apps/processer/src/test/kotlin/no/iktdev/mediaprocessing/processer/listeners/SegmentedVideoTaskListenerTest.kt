package no.iktdev.mediaprocessing.processer.listeners

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.ffmpeg.data.FFprobeFormat
import no.iktdev.mediaprocessing.processer.CoordinatorClient
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.processer.WorkingFile
import no.iktdev.mediaprocessing.processer.WorkingFolder
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.processer.runners.ProbeRunner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.runners.segment.SegmentConcatRunner
import no.iktdev.mediaprocessing.processer.runners.segment.SegmentEncodeRunner
import no.iktdev.mediaprocessing.processer.segment.*
import no.iktdev.mediaprocessing.processer.strategy.EncodingStrategy
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SegmentedVideoTaskListenerTest {

    private val props = ProcesserProperties(
        coordinatorUrl = "http://localhost",
        coordinatorPingOnStartup = false,
        allowOverwrite = true,
        enableSegmentedTaskListener = true
    )

    private val coordinator = mockk<CoordinatorClient>(relaxed = true)
    private val progressCache = mockk<LocalProgressCache>(relaxed = true)
    private val executables = mockk<ExecutablesConfig>(relaxed = true)

    // IMPORTANT: not relaxed
    private val fileUtil = mockk<FileUtil>()

    private lateinit var listener: SegmentedVideoTaskListener
    lateinit var reporter: TaskReporter

    private val workFolder = WorkingFolder()

    @BeforeEach
    fun cleanTestDir() {
        if (workFolder.exists()) workFolder.deleteRecursively()
        workFolder.mkdirs()

        listener = SegmentedVideoTaskListener(
            coordinatorWebClient = coordinator,
            localProgress = progressCache,
            executableConfig = executables,
            fileUtil = fileUtil,
            props
        )
        reporter = mockk<TaskReporter>(relaxed = true)

        every { reporter.markClaimed(any(), any()) } returns no.iktdev.eventi.tasks.Result.Success
        every { reporter.publishEvent(any()) } returns no.iktdev.eventi.tasks.Result.Success
        every { reporter.markCompleted(any()) } returns no.iktdev.eventi.tasks.Result.Success
        every { reporter.updateProgress(any(), any(), any()) } returns no.iktdev.eventi.tasks.Result.Success
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private fun fakeEncodeTask(): EncodeTask {
        val task =  EncodeTask(
            data = EncodeData(
                inputFile = workFolder.using("input.mp4").absolutePath,
                outputFileName = "out.mp4",
                arguments = listOf("-c:v", "libx264")
            )
        )
        task.newReferenceId()
        assertTrue { task.referenceId != null }
        return task
    }


    private fun fakeContext(): SegmentedRunnerContext =
        SegmentedRunnerContext(
            task = fakeEncodeTask(),
            input = workFolder.using("input.mp4"),
            output = workFolder.using("out", "out.mp4"),
            logDirectory = File(workFolder, "logs").apply { mkdirs() },
            checkpointFile = workFolder.using("out", "checkpoints.json"),
            taskStartTime = 0,
            args = listOf("-c:v", "libx264")
        )

    private fun fakeFormat(durationSeconds: Double) = FFprobeFormat(
        filename = null,
        nb_streams = null,
        nb_programs = null,
        format_name = null,
        format_long_name = null,
        start_time = null,
        duration = durationSeconds.toString(),
        size = null,
        bit_rate = null,
        probe_score = null,
        tags = null
    )

    private fun fakeProbePayload(durationSeconds: Double = 60.0) =
        ProbeRunner.ProbePayload(
            format = fakeFormat(durationSeconds),
            videoStreams = emptyList(),
            audioStreams = emptyList(),
        )

    // ---------------------------------------------------------
    // TEST 1 — Restart-sikkerhet
    // ---------------------------------------------------------

    @Test
    fun only_uncompleted_segments_are_processed() = runTest {
        val task = fakeEncodeTask()

        val output = workFolder.using("out", "out.mp4").apply { parentFile.mkdirs() }
        val checkpointFile = workFolder.using("out", "checkpoints.json")

        every { fileUtil.getTemporaryStoreFile(any()) } returns output
        every { fileUtil.getLogDirectory() } returns File(workFolder, "logs").apply { mkdirs() }

        mockkConstructor(ProbeRunner::class)
        coEvery { anyConstructed<ProbeRunner>().run() } returns RunnerResult.Success(fakeProbePayload(180.0))

        val s0 = Segment(0, 0.0, 60.0, workFolder.using("seg0.mp4"))
        val s1 = Segment(1, 60.0, 60.0, workFolder.using("seg1.mp4"))
        val s2 = Segment(2, 120.0, 60.0, workFolder.using("seg2.mp4"))

        mockkConstructor(SegmentPlanner::class)
        every { anyConstructed<SegmentPlanner>().plan(any(), any(), any()) } returns listOf(s0, s1, s2)

        checkpointFile.writeText("""{"completed":[0,1]}""")

        s0.output.apply { parentFile.mkdirs(); writeText("done") }
        s1.output.apply { parentFile.mkdirs(); writeText("done") }

        mockkConstructor(SegmentEncodeRunner::class)
        coEvery { anyConstructed<SegmentEncodeRunner>().run() } returns RunnerResult.Success(
            SegmentEncodeRunner.SegmentEncodePayload(2, WorkingFile("seg2.mp4"))
        )

        mockkConstructor(SegmentConcatRunner::class)
        coEvery { anyConstructed<SegmentConcatRunner>().run() } returns RunnerResult.Success(
            SegmentConcatRunner.ConcatPayload(output, null)
        )


        listener.accept(task, reporter)
        listener.currentJob?.join()


        coVerify(exactly = 1) { anyConstructed<SegmentEncodeRunner>().run() }
        coVerify(exactly = 1) { anyConstructed<SegmentConcatRunner>().run() }
    }

    // ---------------------------------------------------------
    // TEST 2 — Stale file handling
    // ---------------------------------------------------------

    @Test
    fun stale_segment_file_is_deleted() = runTest {
        val stale = workFolder.using("seg1.mp4").apply {
            parentFile.mkdirs()
            writeText("old")
        }

        val segment = Segment(1, 60.0, 60.0, stale)
        val checkpoint = SegmentCheckpointStore.Checkpoint(mutableSetOf())

        mockkConstructor(SegmentEncodeRunner::class)
        coEvery { anyConstructed<SegmentEncodeRunner>().run() } returns RunnerResult.Success(
            SegmentEncodeRunner.SegmentEncodePayload(1, workFolder.using("seg1.mp4"))
        )

        listener.processSegment(
            segment = segment,
            checkpointStore = mockk(relaxed = true),
            checkpoint = checkpoint,
            segments = listOf(segment),
            ctx = fakeContext()
        )

        assertFalse(stale.exists())
    }

    // ---------------------------------------------------------
    // TEST 3 — Concat always runs
    // ---------------------------------------------------------

    @Test
    fun concat_is_called_after_all_segments() = runTest {
        val task = fakeEncodeTask()

        val output = workFolder.using("out", "out.mp4").apply { parentFile.mkdirs() }

        every { fileUtil.getTemporaryStoreFile(any()) } returns output
        every { fileUtil.getLogDirectory() } returns File(workFolder, "logs").apply { mkdirs() }

        mockkConstructor(ProbeRunner::class)
        coEvery { anyConstructed<ProbeRunner>().run() } returns RunnerResult.Success(fakeProbePayload(120.0))

        mockkConstructor(SegmentPlanner::class)
        every { anyConstructed<SegmentPlanner>().plan(any(), any(), any()) } returns listOf(
            Segment(0, 0.0, 60.0, workFolder.using("seg0.mp4")),
            Segment(1, 60.0, 60.0, workFolder.using("seg1.mp4"))
        )

        mockkConstructor(SegmentEncodeRunner::class)
        coEvery { anyConstructed<SegmentEncodeRunner>().run() } returnsMany listOf(
            RunnerResult.Success(SegmentEncodeRunner.SegmentEncodePayload(0, workFolder.using("seg0.mp4"))),
            RunnerResult.Success(SegmentEncodeRunner.SegmentEncodePayload(1, workFolder.using("seg1.mp4")))
        )

        mockkConstructor(SegmentConcatRunner::class)
        coEvery { anyConstructed<SegmentConcatRunner>().run() } returns RunnerResult.Success(
            SegmentConcatRunner.ConcatPayload(output, null)
        )

        listener.accept(task, reporter)
        listener.currentJob?.join()

        coVerify(exactly = 1) { anyConstructed<SegmentConcatRunner>().run() }
    }

    // ---------------------------------------------------------
    // TEST 4 — Log merging
    // ---------------------------------------------------------

    @Test
    fun collectLogs_merges_in_timestamp_order() {
        val dir = File(workFolder, "logs").apply { mkdirs() }

        val a = dir.using("a.log").apply { writeText("A"); setLastModified(1000) }
        val b = dir.using("b.log").apply { writeText("B"); setLastModified(2000) }

        val merged = listener.collectLogs(dir, 0)
        val text = merged.readText()

        assertTrue(text.indexOf("A") < text.indexOf("B"))
    }

    @Test
    fun strategy_returns_segmented_for_video_reencode() {
        val task = EncodeTask(
            data = EncodeData(
                inputFile = "input.mp4",
                outputFileName = "out.mp4",
                arguments = listOf("-c:v", "libx264")
            )
        ).apply { newReferenceId() }

        val strategy = listener.getEncodeStrategy(task)

        assertEquals(EncodingStrategy.Segmented, strategy)
    }

    @Test
    fun segmented_listener_accepts_when_strategy_is_segmented() {
        val task = EncodeTask(
            data = EncodeData(
                inputFile = "input.mp4",
                outputFileName = "out.mp4",
                arguments = listOf("-c:v", "libx264")
            )
        ).newReferenceId()


        val accepted = listener.accept(task, reporter)

        assertTrue(accepted)
    }
}
