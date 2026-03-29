package no.iktdev.mediaprocessing.processer



import com.github.pgreze.process.ProcessResult
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.iktdev.files.FakeFile
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.FfmpegDsl

fun FFmpeg.verifyRunCalled(times: Int = 1) {
    coVerify(exactly = times) { this@verifyRunCalled.run(any(), any()) }
}

fun fakeFFmpeg(resultCode: Int, logFile: IFile = FakeFile("build").using("tests", "ffmpeg.log")): FFmpeg {
    val ff = mockk<FFmpeg>(relaxed = true)

    coEvery { ff.run(any(), any()) } returns Unit

    every { ff.result } returns ProcessResult(
        resultCode = resultCode,
        output = emptyList()
    )

    every { ff.logFile } returns logFile

    return ff
}

fun FFmpeg.captureFfmpegDsl(): CapturingSlot<FfmpegDsl> {
    val slot = CapturingSlot<FfmpegDsl>()

    coEvery {
        this@captureFfmpegDsl.run(capture(slot), any())
    } returns Unit

    return slot
}