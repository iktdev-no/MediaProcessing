package no.iktdev.mediaprocessing.processer.runners

import com.github.pgreze.process.ProcessResult
import io.mockk.*
import kotlinx.coroutines.test.runTest
import no.iktdev.files.FakeFile
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.FfmpegDsl
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.AudioStreamConfig
import no.iktdev.mediaprocessing.ffmpeg.model.AudioTrack
import no.iktdev.mediaprocessing.processer.TestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AudioVideoMergeRunnerTest : TestBase() {

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private fun fakeFFmpeg(resultCode: Int): FFmpeg {
        val ff = mockk<FFmpeg>(relaxed = true)

        coEvery { ff.run(any<FfmpegDsl>()) } returns Unit

        every { ff.result } returns ProcessResult(
            resultCode = resultCode,
            output = emptyList()
        )

        return ff
    }

    private fun fakeAudioPayload(
        index: Int,
        folder: IFile,
        meta: AudioStreamConfig = AudioStreamConfig(
            streamIndex = index,
            language = "eng",
            title = "Track $index",
            default = index == 0
        )
    ): AudioEncodeRunner.AudioEncodePayload {

        val file = folder.using("audio$index.mka").apply { writeText("dummy") }

        return AudioEncodeRunner.AudioEncodePayload(
            output = file,
            meta = meta
        )
    }


    // ---------------------------------------------------------
    // TEST 1 — Success case
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når FFmpeg merge-operasjonen fullfører uten feil (resultCode = 0)
        Hvis AudioVideoMergeRunner.run() kalles
        Så:
            Skal RunnerResult.Success returneres med output-filen
    """)
    fun success_case_returns_success_payload() = runTest {
        val video = workFolder.using("video.mp4").apply { writeText("dummy") }
        val audio = fakeAudioPayload(0, workFolder)

        val output = workFolder.using("merged.mp4")
        val ffmpeg = fakeFFmpeg(0)

        val runner = AudioVideoMergeRunner(
            videoFile = video,
            audioFiles = listOf(audio),
            output = output,
            ffmpegInstance = ffmpeg
        )

        val result = runner.run()

        assertTrue(result is RunnerResult.Success<*>)
        val payload = (result as RunnerResult.Success<AudioVideoMergeRunner.MergePayload>).payload

        assertEquals(output, payload.output)
        coVerify(exactly = 1) { ffmpeg.run(any<FfmpegDsl>()) }
    }

    // ---------------------------------------------------------
    // TEST 2 — Failure case
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når FFmpeg merge-operasjonen feiler (resultCode != 0)
        Hvis AudioVideoMergeRunner.run() kalles
        Så:
            Skal RunnerResult.Reject returneres med riktig feilmelding
    """)
    fun failure_case_returns_reject() = runTest {
        val video = workFolder.using("video.mp4").apply { writeText("dummy") }
        val audio = fakeAudioPayload(0, workFolder)

        val output = workFolder.using("merged.mp4")
        val ffmpeg = fakeFFmpeg(127)

        val runner = AudioVideoMergeRunner(
            videoFile = video,
            audioFiles = listOf(audio),
            output = output,
            ffmpegInstance = ffmpeg
        )

        val result = runner.run()

        assertTrue(result is RunnerResult.Reject)
        val reason = (result as RunnerResult.Reject).reason

        assertEquals("Final merge failed with code 127", reason)
        coVerify(exactly = 1) { ffmpeg.run(any<FfmpegDsl>()) }
    }

    // ---------------------------------------------------------
    // TEST 3 — FFmpeg arguments are correct
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når AudioVideoMergeRunner bygges
        Hvis run() kalles
        Så:
            Skal MpegArgument inneholde korrekt input-video, audio-inputs,
            metadata og copy-flags
    """)
    fun verifies_correct_ffmpeg_arguments() = runTest {
        val video = workFolder.using("video.mp4").apply { writeText("dummy") }
        val audio0 = fakeAudioPayload(
            index = 0,
            folder = workFolder,
            meta = AudioStreamConfig(
                streamIndex = 0,
                language = "eng",
                title = "English",
                default = true,
                forced = false,
                commentary = false,
                descriptive = false,
                hearingImpaired = false,
                original = false
            )
        )

        val audio1 = fakeAudioPayload(
            index = 0,
            folder = workFolder,
            meta = AudioStreamConfig(
                streamIndex = 0,
                language = "jpn",
                title = "Japanese",
                default = false,
                forced = true,
                commentary = false,
                descriptive = false,
                hearingImpaired = false,
                original = false
            )
        )


        val output = workFolder.using("merged.mp4")
        val ffmpeg = fakeFFmpeg(0)

        val slotArgs = slot<FfmpegDsl>()
        coEvery { ffmpeg.run(capture(slotArgs)) } returns Unit

        val runner = AudioVideoMergeRunner(
            videoFile = video,
            audioFiles = listOf(audio0, audio1),
            output = output,
            ffmpegInstance = ffmpeg
        )

        runner.run()

        val built = slotArgs.captured.build()

// Video input
        assertTrue(built.contains(video.absolutePath))

// Audio inputs
        assertTrue(built.contains(audio0.output.absolutePath))
        assertTrue(built.contains(audio1.output.absolutePath))

// --- METADATA ---

// Audio0 metadata (stream 0)
        assertTrue(built.contains("-metadata:s:a:0"))
        assertTrue(built.contains("language=eng"))
        assertTrue(built.contains("title=English"))
        assertTrue(built.contains("-disposition:a:0"))
        assertTrue(built.contains("default"))

// Audio1 metadata (stream 1)
        assertTrue(built.contains("-metadata:s:a:1"))
        assertTrue(built.contains("language=jpn"))
        assertTrue(built.contains("title=Japanese"))
        assertTrue(built.contains("-disposition:a:1"))
        assertTrue(built.contains("forced"))

// --- MAPPING ---

// Video mapping
        assertTrue(built.contains("-map"))
        assertTrue(built.contains("0:v:0"))

// Audio0 mapping
        assertTrue(built.contains("1:a:0"))

// Audio1 mapping
        assertTrue(built.contains("2:a:0"))   // Viktig: alltid a:0 for separate filer

// --- CODECS ---

// Video codec
        assertTrue(
            built.contains("-c:v:0") || built.contains("-c:v"),
            "Expected either -c:v:0 or -c:v"
        )
        assertTrue(built.contains("copy"))

// Audio codecs
        assertTrue(built.contains("-c:a:0"))
        assertTrue(built.contains("-c:a:1"))
        assertTrue(built.count { it == "copy" } >= 3)

// --- OUTPUT ---

        val outputUsed = slotArgs.captured.outputFile()
        assertTrue(outputUsed.endsWith("merged.mp4"))

    }
}
