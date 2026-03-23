package no.iktdev.mediaprocessing.processer.segment

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.AudioStreamConfig
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.processer.TestBase
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.listeners.FfTaskListener
import no.iktdev.mediaprocessing.processer.runners.AudioEncodeRunner
import no.iktdev.mediaprocessing.processer.runners.AudioVideoMergeRunner
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SegmentedAudioProcessorTest: TestBase() {

    private fun fakeMeta(index: Int): AudioStreamConfig {
        return AudioStreamConfig(
            streamIndex = index,
            codec = AudioCodec.Copy,
            language = "eng",
            title = "Track $index",
            default = index == 0
        )
    }


    @Test
    @DisplayName("""
    Når audio-spor er ferdig i checkpoint
    Hvis output-filen eksisterer
    Så:
        Skal sporet returneres uten re-encoding
""")
    fun `completed audio track is returned without re-encoding`() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }
        val audioFolder = intermediate.using("audio").apply { mkdirs() }

        val outFile = audioFolder.using("track_0.mka").apply { writeText("dummy") }

        val ctx = fakeContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO_CHECKPOINTS.json").apply {
                writeText("""{"completed":[0]}""")
            },
            audioInstructions = listOf(fakeAudioInstruction(workFolder.using("input.mka"), "track_0.mka"))
        )

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<SegmentedProgressListener>(relaxed = true)

        val processor = SegmentedAudioProcessor(ffProvider, progress)

        val result = processor.encodeAudioStreams(ctx)

        assertEquals(1, result.size)
        assertEquals(outFile.absolutePath, result[0].output.absolutePath)
        verify { progress.onAudioProgress(1, 1) }
    }

    @Test
    @DisplayName("""
    Når audio-spor er ferdig i checkpoint
    Hvis output-filen mangler
    Så:
        Skal IllegalStateException kastes
""")
    fun `completed audio track missing file throws`() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }
        intermediate.using("audio").also {
            it.mkdirs()
            val file = it.using("track_0.mka")
            file.asFake()?.changeExist(false)

            val file2 = it.using("track_0.work.mka")
            file2.asFake()?.changeExist(false)
        }


        val ctx = fakeContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO_CHECKPOINTS.json").apply {
                writeText("""{"completed":[0]}""")
            },
            audioInstructions = listOf(fakeAudioInstruction(workFolder.using("input.mka"), "track_0.mka"))
        )

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<SegmentedProgressListener>(relaxed = true)

        val processor = SegmentedAudioProcessor(ffProvider, progress)

        assertThrows<IllegalStateException> {
            processor.encodeAudioStreams(ctx)
        }
    }

    @Test
    @DisplayName("""
    Når audio-spor ikke er ferdig
    Hvis AudioEncodeRunner returnerer Success
    Så:
        Skal checkpoint oppdateres og progress rapporteres
""")
    fun `audio track encodes successfully and updates checkpoint`() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }
        val audioFolder = intermediate.using("audio").apply { mkdirs() }

        val outFile = audioFolder.using("track_0.mka")

        mockkConstructor(AudioEncodeRunner::class)
        coEvery { anyConstructed<AudioEncodeRunner>().run() } returns
                RunnerResult.Success(AudioEncodeRunner.AudioEncodePayload(outFile, fakeMeta(0)))

        val ctx = fakeContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO_CHECKPOINTS.json"),
            audioInstructions = listOf(fakeAudioInstruction(workFolder.using("input.mka"), "track_0.mka"))
        )


        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<SegmentedProgressListener>(relaxed = true)

        val processor = SegmentedAudioProcessor(ffProvider, progress)

        val result = processor.encodeAudioStreams(ctx)

        coVerify { anyConstructed<AudioEncodeRunner>().run() }


        assertEquals(1, result.size)
        verify { progress.onAudioProgress(1, 1) }
    }

    @Test
    @DisplayName("""
    Når audio-spor ikke er ferdig i checkpoint
    Hvis output-filen eksisterer (stale)
    Så:
        Skal stale fil slettes og sporet encodes på nytt
""")
    fun `stale audio file is deleted and re-encoded`() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }
        val audioFolder = intermediate.using("audio").apply { mkdirs() }

        // Lag en stale fil
        val outFile = audioFolder.using("track_0.mka").apply { writeText("stale") }
        assertTrue(outFile.exists())

        // Ingen checkpoint → completed = []
        val ctx = fakeContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO_CHECKPOINTS.json").apply {
                writeText("""{"completed":[]}""")
            },
            audioInstructions = listOf(fakeAudioInstruction(workFolder.using("input.mka"), "track_0.mka"))
        )

        // Mock runner → return success
        mockkConstructor(AudioEncodeRunner::class)
        coEvery { anyConstructed<AudioEncodeRunner>().run() } returns
                RunnerResult.Success(AudioEncodeRunner.AudioEncodePayload(outFile, fakeMeta(0)))

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<SegmentedProgressListener>(relaxed = true)

        val processor = SegmentedAudioProcessor(ffProvider, progress)

        val result = processor.encodeAudioStreams(ctx)

        // Stale file skal være slettet før encoding
        assertFalse(outFile.readText() == "stale")

        // Runner skal ha blitt kjørt
        coVerify { anyConstructed<AudioEncodeRunner>().run() }

        // Progress skal rapporteres
        verify { progress.onAudioProgress(1, 1) }

        // Resultat skal returneres
        assertEquals(1, result.size)
    }


    @Test
    @DisplayName("""
    Når audio-spor ikke er ferdig
    Hvis AudioEncodeRunner returnerer Reject
    Så:
        Skal FfmpegFailedException kastes
""")

    fun `audio encoding failure throws FfmpegFailedException`() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }
        intermediate.using("audio").also {
            it.mkdirs()
            val file = it.using("track_0.mka")
            file.asFake()?.changeExist(false)
        }

        mockkConstructor(AudioEncodeRunner::class)
        coEvery { anyConstructed<AudioEncodeRunner>().run() } returns
                RunnerResult.Reject("boom")

        val fakeFfmpeg = mockk<FFmpeg>(relaxed = true)
        every { fakeFfmpeg.logFile } returns workFolder.using("log.txt")

        val ffProvider = mockk<FfProvider>(relaxed = true)
        every { ffProvider.getFfmpeg(any(), any()) } returns fakeFfmpeg

        val ctx = fakeContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO_CHECKPOINTS.json"),
            audioInstructions = listOf(fakeAudioInstruction(workFolder.using("input.mka"), "track_0.mka"))
        )

        val progress = mockk<SegmentedProgressListener>(relaxed = true)
        val processor = SegmentedAudioProcessor(ffProvider, progress)

        assertThrows<FfTaskListener.FfmpegFailedException> {
            processor.encodeAudioStreams(ctx)
        }
    }

    @Test
    @DisplayName("""
    Når flere audio-spor encodes
    Hvis alle lykkes
    Så:
        Skal progress rapporteres for hvert spor
""")
    fun `progress updates correctly for multiple audio tracks`() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }

        mockkConstructor(AudioEncodeRunner::class)
        coEvery { anyConstructed<AudioEncodeRunner>().run() } returns
                RunnerResult.Success(AudioEncodeRunner.AudioEncodePayload(intermediate.using("dummy.mka"), fakeMeta(0)))

        val ctx = fakeContext().copy(
            intermediateStore = intermediate,
            audioCheckpointFile = intermediate.using("AUDIO_CHECKPOINTS.json"),
            audioInstructions = listOf(
                fakeAudioInstruction(workFolder.using("i0.mka"), "track_0.mka"),
                fakeAudioInstruction(workFolder.using("i1.mka"), "track_1.mka")
            )
        )

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<SegmentedProgressListener>(relaxed = true)

        val processor = SegmentedAudioProcessor(ffProvider, progress)

        processor.encodeAudioStreams(ctx)

        verify { progress.onAudioProgress(1, 2) }
        verify { progress.onAudioProgress(2, 2) }
    }

    @Test
    @DisplayName("""
    Når audio skal merges inn i video
    Hvis AudioVideoMergeRunner returnerer Success
    Så:
        Skal finalOutput returneres og progress settes til 1.0
""")
    fun `merge audio into video succeeds`() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }

        val audioPayload = AudioEncodeRunner.AudioEncodePayload(
            output = intermediate.using("a.mka").apply { writeText("x") },
            meta = fakeMeta(0)
        )

        mockkConstructor(AudioVideoMergeRunner::class)
        coEvery { anyConstructed<AudioVideoMergeRunner>().run() } returns
                RunnerResult.Success(
                    AudioVideoMergeRunner.MergePayload(
                        intermediate.using("final.mkv")
                    )
                )


        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<SegmentedProgressListener>(relaxed = true)

        val ctx = fakeContext().copy(
            intermediateStore = intermediate
        )

        val fakeConcat = ctx.output.parentFile.using("Concat.mp4")

        val finalOutput = ctx.intermediateStore
            .using(ctx.task.data.outputFileName)
            .apply { asFake()!!.changeExist(false) }

        val processor = SegmentedAudioProcessor(ffProvider, progress)

        val result = processor.mergeAudioIntoVideo(ctx, listOf(audioPayload), fakeConcat)

        coVerify { anyConstructed<AudioVideoMergeRunner>().run() }

        verify { progress.onMergeProgress(0.0) }
        verify { progress.onMergeProgress(1.0) }

    }

    @Test
    @DisplayName("""
    Når audio skal merges inn i video
    Hvis AudioVideoMergeRunner returnerer Reject
    Så:
        Skal IllegalStateException kastes
""")

    fun `merge audio into video failure throws`() = runTest {
        val intermediate = workFolder.using("intermediate").apply { mkdirs() }

        val audioPayload = AudioEncodeRunner.AudioEncodePayload(
            output = intermediate.using("a.mka").apply { writeText("x") },
            meta = fakeMeta(0)
        )

        mockkConstructor(AudioVideoMergeRunner::class)
        coEvery { anyConstructed<AudioVideoMergeRunner>().run() } returns
                RunnerResult.Reject("boom")

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<SegmentedProgressListener>(relaxed = true)

        val ctx = fakeContext().copy(
            intermediateStore = intermediate
        )

        val fakeConcat = ctx.output.parentFile.using("Concat.mp4")

        val finalOutput = ctx.intermediateStore
            .using(ctx.task.data.outputFileName)
            .apply { asFake()!!.changeExist(false) }

        val processor = SegmentedAudioProcessor(ffProvider, progress)


        assertThrows<IllegalStateException> {
            processor.mergeAudioIntoVideo(ctx, listOf(audioPayload), fakeConcat)
        }
    }


}