package no.iktdev.mediaprocessing.processer.runners.segment

import com.github.pgreze.process.ProcessResult
import org.junit.jupiter.api.Assertions.*


import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.arguments.MpegArgument
import no.iktdev.mediaprocessing.processer.WorkingFile
import no.iktdev.mediaprocessing.processer.WorkingFolder
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.segment.Segment
import org.junit.jupiter.api.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SegmentConcatRunnerTest {

    private val testRoot = File("build/test-run")

    @BeforeEach
    fun clean() {
        if (testRoot.exists()) testRoot.deleteRecursively()
        testRoot.mkdirs()
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private fun fakeSegment(index: Int, name: String = "seg$index.mp4") =
        Segment(
            index = index,
            start = index * 10.0,
            duration = 10.0,
            output = WorkingFile(name).apply {
                parentFile.mkdirs()
                writeText("dummy")
            }
        )

    private fun fakeFFmpeg(resultCode: Int, logFile: File = WorkingFile("ffmpeg.log")): FFmpeg {
        val ff = mockk<FFmpeg>(relaxed = true)

        coEvery { ff.run(any()) } returns Unit

        every { ff.result } returns ProcessResult(
            resultCode = resultCode,
            output = emptyList(),
        )

        every { ff.logFile } returns logFile

        return ff
    }


    // ---------------------------------------------------------
    // TEST 1 — Success case
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når FFmpeg concat-operasjonen fullfører uten feil (resultCode = 0)
        Hvis SegmentConcatRunner.run() kalles
        Så:
            Skal RunnerResult.Success returneres med output og logFile
    """)
    fun success_case_returns_success_payload() = runTest {
        val seg0 = fakeSegment(0)
        val seg1 = fakeSegment(1)
        val output = WorkingFolder().using("out.mp4")
        val logFile = WorkingFile("concat.log").apply { writeText("log") }

        val ffmpeg = fakeFFmpeg(0, logFile)

        val runner = SegmentConcatRunner(
            segments = listOf(seg0, seg1),
            intermediateStore = WorkingFolder(),
            output = output,
            ffmpegInstance = ffmpeg
        )

        val result = runner.run()

        assertTrue(result is RunnerResult.Success<*>)

        val payload = (result as RunnerResult.Success<SegmentConcatRunner.ConcatPayload>).payload

        assertEquals(output, payload.output)
        assertEquals(logFile, payload.logFile)

        coVerify(exactly = 1) { ffmpeg.run(any()) }
    }

    // ---------------------------------------------------------
    // TEST 2 — Failure case
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når FFmpeg concat-operasjonen feiler (resultCode != 0)
        Hvis SegmentConcatRunner.run() kalles
        Så:
            Skal RunnerResult.Reject returneres med riktig feilmelding
    """)
    fun failure_case_returns_reject() = runTest {
        val seg0 = fakeSegment(0)
        val output = WorkingFolder().using("out.mp4")

        val ffmpeg = fakeFFmpeg(127)

        val runner = SegmentConcatRunner(
            segments = listOf(seg0),
            intermediateStore = WorkingFolder(),
            output = output,
            ffmpegInstance = ffmpeg
        )

        val result = runner.run()

        assertTrue(result is RunnerResult.Reject)

        val reason = (result as RunnerResult.Reject).reason
        assertEquals("Concat failed with code 127", reason)

        coVerify(exactly = 1) { ffmpeg.run(any()) }
    }

    // ---------------------------------------------------------
    // TEST 3 — concat_list.txt is generated correctly
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når SegmentConcatRunner bygges
        Hvis run() kalles
        Så:
            Skal concat_list.txt inneholde alle segment-filene i riktig format
    """)
    fun concat_list_file_is_generated_correctly() = runTest {
        val seg0 = fakeSegment(0)
        val seg1 = fakeSegment(1)
        val output = WorkingFolder().using("out.mp4")

        val ffmpeg = fakeFFmpeg(0)

        val runner = SegmentConcatRunner(
            segments = listOf(seg0, seg1),
            intermediateStore = WorkingFolder(),
            output = output,
            ffmpegInstance = ffmpeg
        )

        runner.run()

        val listFile = File(output.parentFile, "out - CONCAT_LIST.txt")
        assertTrue(listFile.exists())

        val text = listFile.readText().trim()

        assertEquals(
            """
            file '${seg0.output.absolutePath}'
            file '${seg1.output.absolutePath}'
            """.trimIndent(),
            text
        )
    }

    // ---------------------------------------------------------
    // TEST 4 — FFmpeg arguments are correct
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når SegmentConcatRunner bygges
        Hvis run() kalles
        Så:
            Skal MpegArgument inneholde korrekt input, output og concat-argumenter
    """)
    fun verifies_correct_ffmpeg_arguments() = runTest {
        val seg0 = fakeSegment(0)
        val seg1 = fakeSegment(1)
        val output = WorkingFolder().using("out.mp4")

        val ffmpeg = fakeFFmpeg(0)

        val slotArgs = slot<MpegArgument>()
        coEvery { ffmpeg.run(capture(slotArgs)) } returns Unit

        val runner = SegmentConcatRunner(
            segments = listOf(seg0, seg1),
            intermediateStore = WorkingFolder(),
            output = output,
            ffmpegInstance = ffmpeg
        )

        runner.run()

        val built = slotArgs.captured.build()

        // input
        assertTrue("-i" in built)
        val listFile = File(output.parentFile, "out - CONCAT_LIST.txt")
        assertTrue(listFile.absolutePath in built)

        // concat flags
        assertTrue("-f" in built)
        assertTrue("concat" in built)
        assertTrue("-safe" in built)
        assertTrue("0" in built)
        assertTrue("-c" in built)
        assertTrue("copy" in built)

        // output
        val outputUsed = slotArgs.captured.getOutputFileUsed()
        assertTrue(outputUsed in built)
    }
}
