package no.iktdev.mediaprocessing.processer.runners.segment

import com.github.pgreze.process.ProcessResult
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.arguments.MpegArgument
import no.iktdev.mediaprocessing.processer.WorkingFile
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.segment.Segment
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SegmentEncodeRunnerTest {

    private val testRoot = File("build/test-run")

    @BeforeEach
    fun clean() {
        if (testRoot.exists()) testRoot.deleteRecursively()
        testRoot.mkdirs()
    }

    private fun fakeSegment(index: Int = 0) =
        Segment(
            index = index,
            start = 10.0,
            duration = 5.0,
            output = WorkingFile("seg$index.mp4")
        )

    private fun fakeFFmpeg(resultCode: Int): FFmpeg {
        val ff = mockk<FFmpeg>(relaxed = true)

        coEvery { ff.run(any()) } returns Unit

        every { ff.result } returns ProcessResult(
            resultCode = resultCode,
            output = emptyList()
        )

        return ff
    }


    @Test
    @DisplayName("""
    Når SegmentEncodeRunner kjører et segment og FFmpeg returnerer resultCode 0
    Hvis run() kalles
    Så:
        Skal RunnerResult.Success returneres med korrekt index og output-fil
    """)

    fun success_case_returns_success_payload() = runTest {
        val segment = fakeSegment(2)
        val input = WorkingFile("input.mp4").apply { writeText("dummy") }
        val ffmpeg = fakeFFmpeg(0)

        val runner = SegmentEncodeRunner(
            segment = segment,
            input = input,
            args = listOf("-c:v", "libx264"),
            ffmpegInstance = ffmpeg
        )

        val result = runner.run()

        assertTrue(result is RunnerResult.Success<*>)

        val payload = (result as RunnerResult.Success<SegmentEncodeRunner.SegmentEncodePayload>).payload

        assertEquals(2, payload.index)
        assertEquals(segment.output, payload.output)

        coVerify(exactly = 1) { ffmpeg.run(any()) }
    }

    @Test
    @DisplayName("""
    Når FFmpeg feiler under encoding av segmentet (resultCode != 0)
    Hvis run() kalles
    Så:
        Skal RunnerResult.Reject returneres med riktig feilmelding
    """)
    fun failure_case_returns_reject() = runTest {
        val segment = fakeSegment(1)
        val input = WorkingFile("input.mp4").apply { writeText("dummy") }
        val ffmpeg = fakeFFmpeg(127)

        val runner = SegmentEncodeRunner(
            segment = segment,
            input = input,
            args = listOf("-c:v", "libx264"),
            ffmpegInstance = ffmpeg
        )

        val result = runner.run()

        assertTrue(result is RunnerResult.Reject)

        val reason = (result as RunnerResult.Reject).reason
        assertEquals("Segment 1 failed with code 127", reason)

        coVerify(exactly = 1) { ffmpeg.run(any()) }
    }

    @Test
    @DisplayName("""
    Når SegmentEncodeRunner bygges
    Hvis run() kalles
    Så:
        Skal MpegArgument inneholde korrekt input, output, -ss og -t
""")
    fun verifies_correct_ffmpeg_arguments() = runTest {
        val segment = fakeSegment(0)
        val input = WorkingFile("input.mp4").apply { writeText("dummy") }
        val ffmpeg = fakeFFmpeg(0)

        val slotArgs = slot<MpegArgument>()
        coEvery { ffmpeg.run(capture(slotArgs)) } returns Unit

        val runner = SegmentEncodeRunner(
            segment = segment,
            input = input,
            args = listOf("-c:v", "libx264"),
            ffmpegInstance = ffmpeg
        )

        runner.run()

        val built = slotArgs.captured.build()

        // input
        assertTrue("-i" in built)
        assertTrue(input.absolutePath in built)

        // segment timing
        assertTrue("-ss" in built)
        assertTrue("10.0" in built)
        assertTrue("-t" in built)
        assertTrue("5.0" in built)

        // user args
        assertTrue("-c:v" in built)
        assertTrue("libx264" in built)

        // output
        val outputUsed = slotArgs.captured.getOutputFileUsed()
        assertTrue(outputUsed in built)
    }

}

