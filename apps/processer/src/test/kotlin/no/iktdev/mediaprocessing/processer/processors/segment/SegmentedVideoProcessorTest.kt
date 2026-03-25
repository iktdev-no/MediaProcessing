package no.iktdev.mediaprocessing.processer.processors.segment

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import kotlinx.coroutines.test.runTest
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.processer.TestBase
import no.iktdev.mediaprocessing.processer.context.CheckpointStore
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.listeners.FfTaskListener
import no.iktdev.mediaprocessing.processer.progress.SegmentedProgressListener
import no.iktdev.mediaprocessing.processer.runners.RunnerResult
import no.iktdev.mediaprocessing.processer.runners.segment.SegmentEncodeRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SegmentedVideoProcessorTest: TestBase() {



    @DisplayName(
        """
    Når segmentfil finnes men ikke er checkpointet
    Hvis processSegment kjøres
    Så skal stale fil slettes
    """
    )
    @Test
    fun stale_segment_file_is_deleted() = runTest {
        // Arrange
        val stale = workFolder.resolve("seg1.mp4").apply {
            parentFile.mkdirs()
            writeText("old")
        }

        val segment = Segment(index = 1, start = 0.0, duration = 60.0, output = stale)
        val checkpoint = CheckpointStore.Checkpoint(mutableSetOf())

        mockkConstructor(SegmentEncodeRunner::class)
        coEvery { anyConstructed<SegmentEncodeRunner>().run() } returns
                RunnerResult.Success(SegmentEncodeRunner.SegmentEncodePayload(1, stale))

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<SegmentedProgressListener>(relaxed = true)

        val processor = SegmentedVideoProcessor(ffProvider, progress)

        // Act
        processor.processSegment(
            segment = segment,
            checkpointStore = CheckpointStore(workFolder.resolve("cp.json")),
            checkpoint = checkpoint,
            segments = listOf(segment),
            ctx = fakeSegmentContext()
        )

        // Assert
        assertFalse(stale.exists())
    }

    @DisplayName(
        """
    Når segment er markert som ferdig i checkpoint
    Hvis outputfilen finnes
    Så skal processSegment returnere uten å encode
    """
    )
    @Test
    fun checkpointed_segment_is_skipped() = runTest {
        // Arrange
        val file = workFolder.resolve("seg1.mp4").apply {
            parentFile.mkdirs()
            writeText("ok")
        }

        val segment = Segment(1, 0.0, 60.0, file)
        val checkpoint = CheckpointStore.Checkpoint(mutableSetOf(1))

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<SegmentedProgressListener>(relaxed = true)

        mockkConstructor(SegmentEncodeRunner::class)

        val processor = SegmentedVideoProcessor(ffProvider, progress)

        // Act
        processor.processSegment(
            segment = segment,
            checkpointStore = mockk(relaxed = true),
            checkpoint = checkpoint,
            segments = listOf(segment),
            ctx = fakeSegmentContext()
        )

        // Assert
        coVerify(exactly = 0) { anyConstructed<SegmentEncodeRunner>().run() }
    }

    @DisplayName(
        """
    Når encoding lykkes
    Hvis segment markeres som ferdig
    Så skal onVideoProgress kalles med korrekt tid
    """
    )
    @Test
    fun progress_listener_called_with_correct_values() = runTest {
        // Arrange
        val out = workFolder.resolve("seg1.mp4").apply {
            parentFile.mkdirs()
            writeText("dummy")
        }

        val segment = Segment(1, 0.0, 60.0, out)
        val cpFile = workFolder.resolve("cp.json")
        val store = CheckpointStore(cpFile)
        val checkpoint = store.load()

        mockkConstructor(SegmentEncodeRunner::class)
        coEvery { anyConstructed<SegmentEncodeRunner>().run() } returns
                RunnerResult.Success(SegmentEncodeRunner.SegmentEncodePayload(1, out))

        val ffProvider = mockk<FfProvider>(relaxed = true)
        val progress = mockk<SegmentedProgressListener>(relaxed = true)

        val processor = SegmentedVideoProcessor(ffProvider, progress)

        // Act
        processor.processSegment(
            segment = segment,
            checkpointStore = store,
            checkpoint = checkpoint,
            segments = listOf(segment),
            ctx = fakeSegmentContext()
        )

        // Assert
        coVerify {
            progress.onVideoProgress(
                doneSeconds = 60.0,
                totalSeconds = 60.0
            )
        }
    }

    @Test
    @DisplayName("""
    Når encoding feiler
    Hvis SegmentEncodeRunner returnerer Reject
    Så skal FfmpegFailedException kastes
""")
    fun encoding_failure_throws_exception() = runTest {
        // Arrange
        val out = workFolder.using("seg1.mp4").apply {
            parentFile.mkdirs()
            writeText("dummy")
        }

        val segment = Segment(1, 0.0, 60.0, out)

        // Mock runner
        mockkConstructor(SegmentEncodeRunner::class)
        coEvery { anyConstructed<SegmentEncodeRunner>().run() } returns
                RunnerResult.Reject("boom")

        // Mock ffmpeg provider
        val ffProvider = mockk<FfProvider>(relaxed = true)
        val fakeFfmpeg = mockk<FFmpeg>(relaxed = true)
        every { ffProvider.getFfmpeg(any(), any()) } returns fakeFfmpeg

        val progress = mockk<SegmentedProgressListener>(relaxed = true)
        val processor = SegmentedVideoProcessor(ffProvider, progress)

        val ctx = fakeSegmentContext()

        // Act + Assert
        assertThrows<FfTaskListener.FfmpegFailedException> {
            processor.processSegment(
                segment = segment,
                checkpointStore = CheckpointStore(workFolder.using("cp.json")),
                checkpoint = CheckpointStore.Checkpoint(),
                segments = listOf(segment),
                ctx = ctx
            )
        }
    }



}