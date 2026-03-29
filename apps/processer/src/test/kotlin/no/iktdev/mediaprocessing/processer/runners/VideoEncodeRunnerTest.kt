package no.iktdev.mediaprocessing.processer.runners

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.processer.TestBase
import no.iktdev.mediaprocessing.processer.captureFfmpegDsl
import no.iktdev.mediaprocessing.processer.fakeFFmpeg
import no.iktdev.mediaprocessing.processer.verifyRunCalled
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*


@OptIn(ExperimentalCoroutinesApi::class)
class VideoEncodeRunnerTest: TestBase() {

        private fun fakeInput(): IFile =
            workFolder.using("input.mkv").apply { writeText("dummy") }

        private fun fakeOutput(): IFile =
            workFolder.using("output.mkv")

        private fun fakeArgs(): List<String> =
            listOf(
                "-y",
                "-nostdin",
                "-nostats",
                "-hide_banner",
                "-i",
                "build/tests/input.mkv",
                "-c:v:0",
                "copy",
                "build/tests/output.work.mkv",
            )

        // ---------------------------------------------------------
        // TEST 1 — Success case
        // ---------------------------------------------------------

        @Test
        @DisplayName(
            """
        Når FFmpeg video-encoding fullfører uten feil (resultCode = 0)
        Hvis VideoEncodeRunner.run() kalles
        Så:
            Skal RunnerResult.Success returneres med output-filen
    """
        )
        fun success_case_returns_success_payload() = runTest {
            val input = fakeInput()
            val output = fakeOutput().apply {
                this.parentFile.asFake()!!.setDirectory()
                this.asFake()!!.setDirectory()
            }

            val ffmpeg = fakeFFmpeg(0)

            val instruct = FFmpegInstructions(
                inputs = InputSection().apply {
                    file(input.absolutePath) {
                        video(0) {
                            codec = VideoCodec.Copy
                        }
                    }
                },
                output = OutputSection("output.mkv").apply {
                    overwrite = true
                    useWorkFile = true
                },
            )

            val runner = VideoEncodeRunner(
                taskId = UUID.randomUUID(),
                videoInstructions = instruct,
                outputDirectory = output.parentFile,
                outputFile = output,
                ffmpegInstance = ffmpeg
            )

            val result = runner.run()

            assertTrue(result is RunnerResult.Success<*>)

            val payload = (result as RunnerResult.Success<VideoEncodeRunner.VideoEncodeResult>).payload

            assertEquals(output, payload.output)

            ffmpeg.verifyRunCalled()
        }

        // ---------------------------------------------------------
        // TEST 2 — Failure case
        // ---------------------------------------------------------

        @Test
        @DisplayName(
            """
        Når FFmpeg video-encoding feiler (resultCode != 0)
        Hvis VideoEncodeRunner.run() kalles
        Så:
            Skal RunnerResult.Reject returneres med riktig feilmelding
    """
        )
        fun failure_case_returns_reject() = runTest {
            val input = fakeInput()
            val output = fakeOutput().apply {
                this.parentFile.asFake()!!.setDirectory()
                this.asFake()!!.setDirectory()
            }

            val ffmpeg = fakeFFmpeg(127)

            val instruct = FFmpegInstructions(
                inputs = InputSection().apply {
                    file(input.absolutePath) {
                        video(0) {
                            codec = VideoCodec.Copy
                        }
                    }
                },
                output = OutputSection("output.mkv").apply {
                    overwrite = true
                    useWorkFile = true
                },
            )

            val runner = VideoEncodeRunner(
                taskId = UUID.randomUUID(),
                videoInstructions = instruct,
                outputDirectory = output.parentFile,
                outputFile = output,
                ffmpegInstance = ffmpeg
            )

            val result = runner.run()

            assertTrue(result is RunnerResult.Reject)

            val reason = (result as RunnerResult.Reject).reason
            assertEquals("Video encode failed with code 127", reason)

            ffmpeg.verifyRunCalled()
        }

        // ---------------------------------------------------------
        // TEST 3 — FFmpeg arguments are correct
        // ---------------------------------------------------------

        @Test
        @DisplayName(
            """
        Når VideoEncodeRunner bygges
        Hvis run() kalles
        Så:
            Skal FfmpegDsl inneholde korrekt input, args og output
    """
        )
        fun verifies_correct_ffmpeg_arguments() = runTest {
            val input = fakeInput()
            val output = fakeOutput().apply {
                this.parentFile.asFake()!!.setDirectory()
                this.asFake()!!.setDirectory()
            }

            val ffmpeg = fakeFFmpeg(0)

            val slotArgs = ffmpeg.captureFfmpegDsl()

            val instruct = FFmpegInstructions(
                inputs = InputSection().apply {
                    file(input.absolutePath) {
                        video(0) {
                            codec = VideoCodec.Copy
                        }
                    }
                },
                output = OutputSection("output.mkv").apply {
                    overwrite = true
                    useWorkFile = true
                },
            )

            val runner = VideoEncodeRunner(
                taskId = UUID.randomUUID(),
                videoInstructions = instruct,
                outputDirectory = output.parentFile,
                outputFile = output,
                ffmpegInstance = ffmpeg
            )

            runner.run()

            val built = slotArgs.captured.build()

            // input
            assertTrue(input.absolutePath in built)

            // args
            fakeArgs().forEach { arg ->
                assertTrue(arg in built)
            }

            // output
            val outputUsed = slotArgs.captured.outputFileUsed()

            // FFmpeg skal bruke en .work-fil
            assertTrue(outputUsed.endsWith(".work.mkv"))

            // Den skal ligge i samme mappe som output
            assertEquals(output.parentFile.absolutePath, IFile(outputUsed).parent)

            // Den skal ha samme base-navn
            assertTrue(outputUsed.contains(output.nameWithoutExtension))
        }
}