package no.iktdev.mediaprocessing.ffmpeg

import com.github.pgreze.process.ProcessResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.FfmpegDsl
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertFalse

class FFmpegTest {

    class MockFFmpeg(
        override val listener: Listener?,
        private val simulateSuccess: Boolean = true,
        private val delayMillis: Long
    ) : FFmpeg(
        executable = "",
        logDir = IFile("/null")
    ) {
        override suspend fun run(command: FfmpegDsl) {
            val input = command.toInstructions().findPrimaryInput()
            inputFile = input
            logFile = IFile("/null/log.txt")

            listener?.onStarted(input)

            // Simuler litt arbeid
            delay(delayMillis)

            result = ProcessResult(
                resultCode = if (simulateSuccess) 0 else 1,
                output = listOf("Simulated ffmpeg output")
            )

            if (!simulateSuccess) {
                listener?.onError(input, "Simulated error")
                return
            }

            listener?.onCompleted(input, command.outputFile())
        }
    }


    @Test
    @DisplayName("Test FFmpeg Mock Success")
    fun scenarioSuccess() = runBlocking {
        val dsl = ffmpeg {
            input("input.mp4") {
                video(0) {
                    map = true
                    codec = VideoCodec.Copy
                }
            }
            output("output.mp4") {
                overwrite = true
                progress = false
                useWorkFile = false
            }
        }

        val listener = object : FFmpeg.Listener {
            var completed: Boolean = false
                private set
            override fun onStarted(inputFile: String) {
                println("Started processing $inputFile")
            }

            override fun onCompleted(inputFile: String, outputFile: String) {
                println("Completed processing $inputFile to $outputFile")
                completed = true
            }

            override fun onProgressChanged(inputFile: String, progress: FfmpegDecodedProgress) {
                println("Progress for $inputFile: $progress")
            }
        }

        val runner = MockFFmpeg(listener, delayMillis = 1000, simulateSuccess = true)
        assertFalse(listener.completed, "Expected onCompleted to be false before run")

        val elapsed = measureTimeMillis {
            runner.run(dsl)
            assertTrue(listener.completed, "Expected onCompleted to be called")
        }

        assertTrue(elapsed >= 1000, "Expected to wait at least 1000 ms, but waited for $elapsed ms")
    }
}