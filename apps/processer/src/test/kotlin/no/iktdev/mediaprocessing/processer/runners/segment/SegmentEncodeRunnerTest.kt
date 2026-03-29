package no.iktdev.mediaprocessing.processer.runners.segment

import com.github.pgreze.process.ProcessResult
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.FfmpegDsl
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.processer.TestBase
import no.iktdev.mediaprocessing.processer.captureFfmpegDsl
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.processors.segment.Segment
import no.iktdev.mediaprocessing.processer.verifyRunCalled
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SegmentEncodeRunnerTest: TestBase() {

    private val testRoot = IFile("build/test-run")

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
            output = workFolder.using("seg$index.mp4")
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
        val input = workFolder.using("input.mp4").apply { writeText("dummy") }
        val ffmpeg = fakeFFmpeg(0)

        val instruct = FFmpegInstructions(
            inputs = InputSection().apply {
                file(input.absolutePath) {
                    video(0) {
                        map = true
                        codec = VideoCodec.H264()
                    }
                }
            },
            output = OutputSection(segment.output.name).apply {
                overwrite = true
                useWorkFile = false
            }
        )



        val runner = SegmentEncodeRunner(
            taskId = UUID.randomUUID(),
            segment = segment,
            videoInstructions = instruct,
            ffmpegInstance = ffmpeg
        )

        val result = runner.run()

        assertTrue(result is RunnerResult.Success<*>)

        val payload = (result as RunnerResult.Success<SegmentEncodeRunner.SegmentEncodePayload>).payload

        assertEquals(2, payload.index)
        assertEquals(segment.output, payload.output)

        ffmpeg.verifyRunCalled()
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
        val input = workFolder.using("input.mp4").apply { writeText("dummy") }
        val ffmpeg = fakeFFmpeg(127)

        val instruct = FFmpegInstructions(
            inputs = InputSection().apply {
                file(input.absolutePath) {
                    video(0) {
                        map = true
                        codec = VideoCodec.H264()
                    }
                }
            },
            output = OutputSection(segment.output.name).apply {
                overwrite = true
                useWorkFile = false
            },
        )


        val runner = SegmentEncodeRunner(
            taskId = UUID.randomUUID(),
            segment = segment,
            videoInstructions = instruct,
            ffmpegInstance = ffmpeg
        )

        val result = runner.run()

        assertTrue(result is RunnerResult.Reject)

        val reason = (result as RunnerResult.Reject).reason
        assertEquals("Segment 1 failed with code 127", reason)

        ffmpeg.verifyRunCalled()
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
        val input = workFolder.using("input.mp4").apply { writeText("dummy") }
        val ffmpeg = fakeFFmpeg(0)

        // ✔ Global capture
        val slotArgs = ffmpeg.captureFfmpegDsl()

        val instruct = FFmpegInstructions(
            inputs = InputSection().apply {
                file(input.absolutePath) {
                    video(0) {
                        map = true
                        codec = VideoCodec.H264()
                    }
                }
            },
            output = OutputSection(segment.output.name).apply {
                overwrite = true
                useWorkFile = false
            },
        )

        val runner = spyk(
            SegmentEncodeRunner(
                taskId = UUID.randomUUID(),
                segment = segment,
                videoInstructions = instruct,
                ffmpegInstance = ffmpeg
            )
        )

        every { runner.useWorkFileForSegments() } returns false

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
        assertTrue(
            built.contains("-c:v:0") || built.contains("-c:v"),
            "Expected either -c:v:0 or -c:v in FFmpeg arguments"
        )

        assertTrue("libx264" in built)

        // output
        val outputUsed = slotArgs.captured.outputFile()
        assertTrue(outputUsed in built)
    }


}

