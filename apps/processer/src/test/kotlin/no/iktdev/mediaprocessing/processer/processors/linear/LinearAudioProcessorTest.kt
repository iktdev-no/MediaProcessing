package no.iktdev.mediaprocessing.processer.processors.linear

import io.mockk.*
import kotlinx.coroutines.test.runTest
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.AudioStreamConfig
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.processer.TestBase
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.progress.LinearProgressListener
import no.iktdev.mediaprocessing.processer.runners.AudioEncodeRunner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LinearAudioProcessorTest : TestBase() {

    private fun fakeMeta(index: Int = 0): AudioStreamConfig {
        return AudioStreamConfig(
            streamIndex = index,
            codec = AudioCodec.Copy(),
            language = "eng",
            title = "Track $index",
            default = index == 0
        )
    }

    private fun fakeInstruction(input: IFile, outputName: String) =
        no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions(
            inputs = InputSection().apply {
                file(input.absolutePath) {
                    audio(0) {
                        map = true
                        codec = AudioCodec.Copy()
                    }
                }
            },
            output = OutputSection(outputName).apply {
                overwrite = true
                useWorkFile = true
            }
        )

    // ---------------------------------------------------------
    // TEST 1 — Completed track returned without re-encoding
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når audio-spor er ferdig i checkpoint
        Hvis output-filen eksisterer
        Så:
            Skal sporet returneres uten re-encoding
    """)
    fun completed_track_is_returned_without_encoding() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }
        val audioFolder = intermediate.using("audio").apply { mkdirs() }

        val outFile = audioFolder.using("track_0.mka").apply { writeText("dummy") }

        val ctx = fakeLinearContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO.json").apply {
                writeText("""{"completed":[0]}""")
            },
            audioInstructions = listOf(fakeInstruction(workFolder.using("input.mka"), "track_0.mka"))
        )

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<LinearProgressListener>(relaxed = true)

        val processor = LinearAudioProcessor(ffProvider, progress)

        val result = processor.encodeAudio(ctx)

        assertEquals(1, result.size)
        assertEquals(outFile.absolutePath, result[0].output.absolutePath)

        verify { progress.onAudioProgress(0, any(), 1) }
    }

    // ---------------------------------------------------------
    // TEST 2 — Completed track missing file throws
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når audio-spor er ferdig i checkpoint
        Hvis output-filen mangler
        Så:
            Skal IllegalStateException kastes
    """)
    fun completed_track_missing_file_throws() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }
        val audioFolder = intermediate.using("audio").apply { mkdirs() }

        val instruct = fakeInstruction(workFolder.using("input.mka"), "track_0.mka")

        val workFile = audioFolder.using(instruct.output?.workFile!!).apply {
            this.asFake()!!.changeExist(false)
        }
        val OutFile = audioFolder.using(instruct.output?.path!!).apply {
            this.asFake()!!.changeExist(false)
        }

        val ctx = fakeLinearContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO.json").apply {
                writeText("""{"completed":[0]}""")
            },
            audioInstructions = listOf(instruct)
        )

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<LinearProgressListener>(relaxed = true)

        val processor = LinearAudioProcessor(ffProvider, progress)

        assertThrows<IllegalStateException> {
            processor.encodeAudio(ctx)
        }
    }

    // ---------------------------------------------------------
    // TEST 3 — Track encodes successfully
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når audio-spor ikke er ferdig
        Hvis AudioEncodeRunner returnerer Success
        Så:
            Skal checkpoint oppdateres og progress rapporteres
    """)
    fun track_encodes_successfully() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }
        val audioFolder = intermediate.using("audio").apply { mkdirs() }

        val outFile = audioFolder.using("track_0.mka")

        mockkConstructor(AudioEncodeRunner::class)
        coEvery { anyConstructed<AudioEncodeRunner>().run() } returns
                RunnerResult.Success(AudioEncodeRunner.AudioEncodePayload(outFile, null, fakeMeta()))

        val ctx = fakeLinearContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO.json"),
            audioInstructions = listOf(fakeInstruction(workFolder.using("input.mka"), "track_0.mka"))
        )

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<LinearProgressListener>(relaxed = true)

        val processor = LinearAudioProcessor(ffProvider, progress)

        val result = processor.encodeAudio(ctx)

        assertEquals(1, result.size)
        coVerify { anyConstructed<AudioEncodeRunner>().run() }
        verify { progress.onAudioProgress(0, any(), 1) }
    }

    // ---------------------------------------------------------
    // TEST 4 — Stale file is deleted and re-encoded
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når audio-spor ikke er ferdig i checkpoint
        Hvis output-filen eksisterer (stale)
        Så:
            Skal stale fil slettes og sporet encodes på nytt
    """)
    fun stale_file_deleted_and_reencoded() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }
        val audioFolder = intermediate.using("audio").apply { mkdirs() }

        val outFile = audioFolder.using("track_0.mka").apply { writeText("stale") }

        val ctx = fakeLinearContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO.json").apply {
                writeText("""{"completed":[]}""")
            },
            audioInstructions = listOf(fakeInstruction(workFolder.using("input.mka"), "track_0.mka"))
        )

        mockkConstructor(AudioEncodeRunner::class)
        coEvery { anyConstructed<AudioEncodeRunner>().run() } returns
                RunnerResult.Success(AudioEncodeRunner.AudioEncodePayload(outFile, null, fakeMeta()))

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<LinearProgressListener>(relaxed = true)

        val processor = LinearAudioProcessor(ffProvider, progress)

        processor.encodeAudio(ctx)

        assertNotEquals("stale", outFile.readText())
        coVerify { anyConstructed<AudioEncodeRunner>().run() }
        verify { progress.onAudioProgress(0, any(), 1) }
    }

    // ---------------------------------------------------------
    // TEST 5 — Reject throws FfmpegFailedException
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når audio-spor ikke er ferdig
        Hvis AudioEncodeRunner returnerer Reject
        Så:
            Skal FfmpegFailedException kastes
    """)
    fun reject_throws_exception() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }

        mockkConstructor(AudioEncodeRunner::class)
        coEvery { anyConstructed<AudioEncodeRunner>().run() } returns
                RunnerResult.Reject("boom")

        val fakeFfmpeg = mockk<FFmpeg>(relaxed = true)
        every { fakeFfmpeg.logFile } returns workFolder.using("log.txt")

        val ffProvider = mockk<FfProvider>(relaxed = true)
        every { ffProvider.getFfmpeg(any(), any()) } returns fakeFfmpeg

        val instruct = fakeInstruction(workFolder.using("input.mka"), "track_0.mka")


        val ctx = fakeLinearContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO.json"),
            audioInstructions = listOf(instruct)
        )

        val progress = mockk<LinearProgressListener>(relaxed = true)
        val processor = LinearAudioProcessor(ffProvider, progress)

        assertThrows<no.iktdev.mediaprocessing.processer.listeners.FfTaskListener.FfmpegFailedException> {
            processor.encodeAudio(ctx)
        }
    }

    // ---------------------------------------------------------
    // TEST 6 — Multiple tracks progress
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når flere audio-spor encodes
        Hvis alle lykkes
        Så:
            Skal progress rapporteres for hvert spor
    """)
    fun multiple_tracks_progress() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }

        mockkConstructor(AudioEncodeRunner::class)
        coEvery { anyConstructed<AudioEncodeRunner>().run() } returns
                RunnerResult.Success(AudioEncodeRunner.AudioEncodePayload(intermediate.using("dummy.mka"), null, fakeMeta()))

        val ctx = fakeLinearContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO.json"),
            audioInstructions = listOf(
                fakeInstruction(workFolder.using("i0.mka"), "track_0.mka"),
                fakeInstruction(workFolder.using("i1.mka"), "track_1.mka")
            )
        )

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<LinearProgressListener>(relaxed = true)

        val processor = LinearAudioProcessor(ffProvider, progress)

        processor.encodeAudio(ctx)

        verify { progress.onAudioProgress(0, any(), 2) }
        verify { progress.onAudioProgress(1, any(), 2) }
    }
}
