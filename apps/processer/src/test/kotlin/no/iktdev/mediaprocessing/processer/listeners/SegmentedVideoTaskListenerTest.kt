package no.iktdev.mediaprocessing.processer.listeners

import com.github.pgreze.process.ProcessResult
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.files.IFile
import no.iktdev.files.UseFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.data.FFprobeFormat
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.processer.CoordinatorClient
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.processer.TestBase
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.processer.context.SegmentedRunnerContext
import no.iktdev.mediaprocessing.processer.processors.segment.Segment
import no.iktdev.mediaprocessing.processer.processors.segment.SegmentPlanner
import no.iktdev.mediaprocessing.processer.processors.segment.SegmentedContextFactory
import no.iktdev.mediaprocessing.processer.runners.AudioVideoMergeRunner
import no.iktdev.mediaprocessing.processer.runners.ProbeRunner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.runners.segment.SegmentConcatRunner
import no.iktdev.mediaprocessing.processer.runners.segment.SegmentEncodeRunner
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.SegmentedEncodeTask
import no.iktdev.mediaprocessing.shared.common.model.task.data.DefaultEncodeData
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class SegmentedVideoTaskListenerTest: TestBase() {

    private val props = ProcesserProperties(
        coordinatorUrl = "http://localhost",
        coordinatorPingOnStartup = false,
        allowOverwrite = true,
        enableSegmentedTaskListener = true
    )

    private val coordinator = mockk<CoordinatorClient>(relaxed = true)
    private val progressCache = mockk<LocalProgressCache>(relaxed = true)
    private val executables = mockk<ExecutablesConfig>(relaxed = true)
    lateinit var ffmpeg: FFmpeg
    // IMPORTANT: not relaxed
    private val fileUtil = mockk<FileUtil>()

    private lateinit var listener: SegmentedVideoTaskListener
    lateinit var reporter: TaskReporter


    @BeforeEach
    fun cleanTestDir() {
        if (workFolder.exists() && workFolder is UseFile) workFolder.deleteRecursively()
        workFolder.mkdirs()

        // 1) Mock FFmpeg constructor
        mockkConstructor(FFmpeg::class)

        // 2) Stub alle FFmpeg-metoder (ikke constructoren)
        coEvery { anyConstructed<FFmpeg>().run(any()) } returns Unit
        every { anyConstructed<FFmpeg>().result } returns ProcessResult(0, emptyList())

        // 3) Mock runner-konstruktører
        mockkConstructor(SegmentPlanner::class)
        mockkConstructor(ProbeRunner::class)
        mockkConstructor(SegmentEncodeRunner::class)
        mockkConstructor(SegmentConcatRunner::class)

        // 4) Opprett listener ETTER mocking
        listener = SegmentedVideoTaskListener(
            coordinatorWebClient = coordinator,
            localProgress = progressCache,
            executableConfig = executables,
            fileUtil = fileUtil,
        )

        reporter = mockk(relaxed = true)

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

    private fun fakeEncodeTask(): SegmentedEncodeTask {
        val input = workFolder.using("input.mp4").absolutePath

        val task = SegmentedEncodeTask(
            data = DefaultEncodeData(
                inputFile = input,
                outputFileName = "out.mp4",
                outputFolderName = "out",

                // NEW DSL-COMPATIBLE VIDEO INSTRUCTION
                videoInstruction = FFmpegInstructions(
                    inputs = InputSection().apply {
                        file(input) {
                            video(0) {
                                map = true
                                codec = VideoCodec.H264()
                            }
                        }
                    },
                    output = OutputSection("out.mp4").apply {
                        overwrite = true
                        useWorkFile = true
                    }
                ),

                // No audio tracks for this test
                audioInstructions = emptyList()
            )
        )

        task.newReferenceId()
        assertTrue { task.referenceId != null }
        return task
    }


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

    @Test
    @DisplayName("""
    Når segmenterte jobber restartes
    Hvis tidligere fullførte segmenter finnes i checkpoint
    Så:
        Kun ufullførte segmenter prosesseres
""")
    fun only_uncompleted_segments_are_processed() = runTest {
        val task = fakeEncodeTask()

        // --- Test-spesifikk mocking ---
        val output = workFolder.using("out", "out.mp4").apply { parentFile.mkdirs() }
        val intermediate = workFolder.using("intermediate", "out").apply { mkdirs() }
        val logs = workFolder.using("logs").apply { mkdirs() }

        every { fileUtil.getTemporaryStoreFolder(any()) } returns intermediate
        every { fileUtil.getLogDirectory() } returns logs

        val checkpointFile = intermediate.using("VIDEO_CHECKPOINTS.json")
        checkpointFile.writeText("""{"completed":[0,1]}""")

        // --- Mock kontekstfabrikken slik at den bruker våre filer ---
        mockkConstructor(SegmentedContextFactory::class)
        every {
            anyConstructed<SegmentedContextFactory>().createContext(any())
        } answers {
            SegmentedRunnerContext(
                task = arg(0),
                input = IFile(task.data.inputFile),
                output = output,
                intermediateStore = intermediate,
                logDirectory = logs,
                videoCheckpointFile = checkpointFile,
                audioCheckpointFile = intermediate.using("AUDIO_CHECKPOINTS.json"),
                taskStartTime = System.currentTimeMillis(),
                videoInstruction = task.data.videoInstruction,
                audioInstructions = task.data.audioInstructions,
            )
        }

        // --- Mock runners ---
        coEvery { anyConstructed<ProbeRunner>().run() } returns
                RunnerResult.Success(fakeProbePayload(180.0))

        val s0 = Segment(0, 0.0, 60.0, intermediate.using("seg0.mp4"))
        val s1 = Segment(1, 60.0, 60.0, intermediate.using("seg1.mp4"))
        val s2 = Segment(2, 120.0, 60.0, intermediate.using("seg2.mp4"))

        every { anyConstructed<SegmentPlanner>().plan(any(), any()) } returns listOf(s0, s1, s2)

        // Marker s0 og s1 som ferdige
        s0.output.apply { parentFile.mkdirs(); writeText("done") }
        s1.output.apply { parentFile.mkdirs(); writeText("done") }

        // Mock encode runner (kun s2 skal kjøres)
        coEvery { anyConstructed<SegmentEncodeRunner>().run() } returns
                RunnerResult.Success(SegmentEncodeRunner.SegmentEncodePayload(2, s2.output))

        // Mock concat
        coEvery { anyConstructed<SegmentConcatRunner>().run() } returns
                RunnerResult.Success(SegmentConcatRunner.ConcatPayload(output, null))

        val concat = workFolder.using("out", "out.noaudio.mp4")
        concat.asFake()!!.changeExist(false)
        val final = workFolder.using("out", "out.mp4")
        final.asFake()!!.changeExist(false)

        mockkConstructor(AudioVideoMergeRunner::class)

        coEvery { anyConstructed<AudioVideoMergeRunner>().run() } returns
                RunnerResult.Success(AudioVideoMergeRunner.MergePayload(final))

        // --- Kjør jobben ---
        listener.accept(task, reporter)
        listener.currentJob?.join()

        // --- Verifisering ---
        coVerify(exactly = 1) { anyConstructed<SegmentEncodeRunner>().run() }
        coVerify(exactly = 1) { anyConstructed<SegmentConcatRunner>().run() }
    }




    @Test
    @DisplayName("""
        Når alle segmenter er ferdig prosessert
        Hvis SegmentEncodeRunner returnerer suksess for alle segmenter
        Så:
            Concat-runner kjøres alltid én gang
    """)
    fun concat_is_called_after_all_segments() = runTest {
        val task = fakeEncodeTask()

        val output = workFolder.using("out", "out.mp4").apply { parentFile.mkdirs() }

        every { fileUtil.getTemporaryStoreFolder(any()) } returns workFolder.using("intermediate")

        every { fileUtil.getLogDirectory() } returns workFolder.using("logs").apply { mkdirs() }

        mockkConstructor(ProbeRunner::class)
        coEvery { anyConstructed<ProbeRunner>().run() } returns RunnerResult.Success(fakeProbePayload(120.0))

        mockkConstructor(SegmentPlanner::class)
        every { anyConstructed<SegmentPlanner>().plan(any(), any()) } returns listOf(
            Segment(0, 0.0, 60.0, workFolder.using("intermediate", "seg0.mp4")),
            Segment(1, 60.0, 60.0, workFolder.using("intermediate", "seg1.mp4"))
        )

        mockkConstructor(SegmentEncodeRunner::class)
        coEvery { anyConstructed<SegmentEncodeRunner>().run() } returnsMany listOf(
            RunnerResult.Success(SegmentEncodeRunner.SegmentEncodePayload(0, workFolder.using("intermediate", "seg0.mp4"))),
            RunnerResult.Success(SegmentEncodeRunner.SegmentEncodePayload(1, workFolder.using("intermediate", "seg1.mp4")))
        )

        mockkConstructor(SegmentConcatRunner::class)
        coEvery { anyConstructed<SegmentConcatRunner>().run() } returns RunnerResult.Success(
            SegmentConcatRunner.ConcatPayload(output, null)
        )

        val concat = workFolder.using("intermediate", "out.noaudio.mp4")
        concat.asFake()!!.changeExist(false)
        val final = workFolder.using("intermediate", "out.mp4")
        final.asFake()!!.changeExist(false)

        mockkConstructor(AudioVideoMergeRunner::class)

        coEvery { anyConstructed<AudioVideoMergeRunner>().run() } returns
                RunnerResult.Success(AudioVideoMergeRunner.MergePayload(final))

        listener.accept(task, reporter)
        listener.currentJob?.join()

        coVerify(exactly = 1) { anyConstructed<SegmentConcatRunner>().run() }
    }

    @Test
    @DisplayName("""
        Når flere loggfiler finnes
        Hvis collectLogs kalles
        Så:
            Loggene merges i stigende timestamp-rekkefølge
    """)
    fun collectLogs_merges_in_timestamp_order() {
        val dir = workFolder.using("logs").apply { mkdirs() }

        dir.using("a.log").also {
            it.writeText("A")
        }
        dir.using("b.log").also {
            it.writeText("B")
        }

        val merged = listener.collectLogs(dir, 0)
        val text = merged.readText()

        assertTrue(text.indexOf("A") < text.indexOf("B"))
    }

}
