package no.iktdev.mediaprocessing.processer.processors.linear

import io.mockk.*
import kotlinx.coroutines.test.runTest
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.processer.TestBase
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.listeners.FfTaskListener
import no.iktdev.mediaprocessing.processer.progress.LinearProgressListener
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.runners.VideoEncodeRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LinearVideoProcessorTest : TestBase() {

    private fun fakeInstruction(input: IFile, outputName: String) =
        no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions(
            inputs = InputSection().apply {
                file(input.absolutePath) {
                    video(0) {
                        codec = VideoCodec.Copy
                    }
                }
            },
            output = OutputSection(outputName).apply {
                overwrite = true
                useWorkFile = true
            }
        )

    // ---------------------------------------------------------
    // TEST 1 — noaudio file exists → return early
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når noaudio-fil finnes
        Hvis encodeVideo() kalles
        Så:
            Skal VideoEncodeRunner IKKE kjøres og filen returneres direkte
    """)
    fun noaudio_file_exists_returns_early() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }

        val output = workFolder.using("movie.mp4")
        val noAudio = output.parentFile.using("movie.noaudio.mp4").apply {
            writeText("dummy")
        }

        val ctx = fakeLinearContext().copy(
            output = output,
            intermediateStore = intermediate,
            videoInstruction = fakeInstruction(workFolder.using("input.mkv"), "movie.mp4")
        )

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<LinearProgressListener>(relaxed = true)

        val processor = LinearVideoProcessor(ffProvider, progress)

        val result = processor.encodeVideo(ctx)

        assertEquals(noAudio.absolutePath, result.output.absolutePath)
        coVerify(exactly = 0) { ffProvider.getFfmpeg(any(), any()) }
    }

    // ---------------------------------------------------------
    // TEST 2 — Successful encoding
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når VideoEncodeRunner returnerer Success
        Hvis encodeVideo() kalles og output filen eksisterer
        Så:
            Skal payload returneres
    """)
    fun encoding_success_returns_payload() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }

        val output = workFolder.using("movie.mp4")
        val noAudio = output.parentFile.using("movie.noaudio.mp4").apply {
            this.asFake()!!.changeExist(false)
        }

        mockkConstructor(VideoEncodeRunner::class)
        coEvery { anyConstructed<VideoEncodeRunner>().run() } returns
                RunnerResult.Success(VideoEncodeRunner.VideoEncodeResult(noAudio))

        val fakeFfmpeg = mockk<FFmpeg>(relaxed = true)

        val ffProvider = mockk<FfProvider>()
        every { ffProvider.getFfmpeg(any(), any()) } returns fakeFfmpeg

        val progress = mockk<LinearProgressListener>(relaxed = true)

        val ctx = fakeLinearContext().copy(
            output = output,
            intermediateStore = intermediate,
            videoInstruction = fakeInstruction(workFolder.using("input.mkv"), "movie.mp4")
        )

        val processor = LinearVideoProcessor(ffProvider, progress)

        val result = processor.encodeVideo(ctx)

        assertEquals(noAudio.absolutePath, result.output.absolutePath)
        coVerify { anyConstructed<VideoEncodeRunner>().run() }
    }

    // ---------------------------------------------------------
    // TEST 3 — Reject → throws FfmpegFailedException
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når VideoEncodeRunner returnerer Reject
        Hvis encodeVideo() kalles
        Så:
            Skal FfmpegFailedException kastes
    """)
    fun encoding_failure_throws_exception() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }

        val output = workFolder.using("movie.mp4")
        val noAudio = output.parentFile.using("movie.noaudio.mp4").apply {
            this.asFake()!!.changeExist(false)
        }

        mockkConstructor(VideoEncodeRunner::class)
        coEvery { anyConstructed<VideoEncodeRunner>().run() } returns
                RunnerResult.Reject("boom")

        val fakeFfmpeg = mockk<FFmpeg>(relaxed = true)
        every { fakeFfmpeg.logFile } returns workFolder.using("log.txt")

        val ffProvider = mockk<FfProvider>()
        every { ffProvider.getFfmpeg(any(), any()) } returns fakeFfmpeg

        val progress = mockk<LinearProgressListener>(relaxed = true)

        val ctx = fakeLinearContext().copy(
            output = output,
            intermediateStore = intermediate,
            videoInstruction = fakeInstruction(workFolder.using("input.mkv"), "movie.mp4")
        )

        val processor = LinearVideoProcessor(ffProvider, progress)

        assertThrows<FfTaskListener.FfmpegFailedException> {
            processor.encodeVideo(ctx)
        }
    }

    // ---------------------------------------------------------
    // TEST 4 — ProgressListener receives progress updates
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når FFmpeg.Listener rapporterer progress
        Hvis encodeVideo() kalles
        Så:
            Skal LinearProgressListener.onVideoProgress kalles
    """)
    fun progress_listener_receives_updates() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }

        val output = workFolder.using("movie.mp4")
        val noAudio = output.parentFile.using("movie.noaudio.mp4").apply {
            this.asFake()!!.changeExist(false)
        }

        val fakeFfmpeg = mockk<FFmpeg>(relaxed = true)

        val progress = mockk<LinearProgressListener>(relaxed = true)

        val ffProvider = mockk<FfProvider>()
        every { ffProvider.getFfmpeg(any<FFmpeg.Listener>(), any<IFile>()) } answers {
            val listener = firstArg<FFmpeg.Listener>()
            listener.onProgressChanged("input", FfmpegDecodedProgress(50, "1", "2", "1.0"))
            fakeFfmpeg
        }

        mockkConstructor(VideoEncodeRunner::class)
        coEvery { anyConstructed<VideoEncodeRunner>().run() } returns
                RunnerResult.Success(VideoEncodeRunner.VideoEncodeResult(noAudio))

        val ctx = fakeLinearContext().copy(
            output = output,
            intermediateStore = intermediate,
            videoInstruction = fakeInstruction(workFolder.using("input.mkv"), "movie.mp4")
        )

        val processor = LinearVideoProcessor(ffProvider, progress)

        processor.encodeVideo(ctx)

        verify { progress.onVideoProgress(match { it.progress == 50 }) }
    }

    // ---------------------------------------------------------
    // TEST 5 — Listener reports 100% on completion
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når FFmpeg.Listener rapporterer onCompleted
        Hvis encodeVideo() kalles
        Så:
            Skal LinearProgressListener.onVideoProgress kalles med 100%
    """)
    fun progress_listener_reports_completion() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }

        val output = workFolder.using("movie.mp4")
        val noAudio = output.parentFile.using("movie.noaudio.mp4").apply {
            this.asFake()!!.changeExist(false)
        }

        val fakeFfmpeg = mockk<FFmpeg>(relaxed = true)

        val progress = mockk<LinearProgressListener>(relaxed = true)

        val ffProvider = mockk<FfProvider>()
        every { ffProvider.getFfmpeg(any<FFmpeg.Listener>(), any<IFile>()) } answers {
            val listener = arg<FFmpeg.Listener>(0)   // listener er første argument
            listener.onCompleted("input", "output")
            fakeFfmpeg
        }

        mockkConstructor(VideoEncodeRunner::class)
        coEvery { anyConstructed<VideoEncodeRunner>().run() } returns
                RunnerResult.Success(VideoEncodeRunner.VideoEncodeResult(noAudio))

        val ctx = fakeLinearContext().copy(
            output = output,
            intermediateStore = intermediate,
            videoInstruction = fakeInstruction(workFolder.using("input.mkv"), "movie.mp4")
        )

        val processor = LinearVideoProcessor(ffProvider, progress)

        processor.encodeVideo(ctx)

        verify { progress.onVideoProgress(match { it.progress == 100 }) }
    }
}
