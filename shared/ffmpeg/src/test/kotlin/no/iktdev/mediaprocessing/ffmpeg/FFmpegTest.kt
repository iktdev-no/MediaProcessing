package no.iktdev.mediaprocessing.ffmpeg

import com.github.pgreze.process.ProcessResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.iktdev.mediaprocessing.ffmpeg.arguments.MpegArgument
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.test.assertFalse

class FFmpegTest {

    class MockFFmpeg(override val listener: Listener, val delayMillis: Long = 500, private val simulateSuccess: Boolean = true) : FFmpeg(executable = "", logDir = File("/null")) {
        override suspend fun run(argument: MpegArgument) {
            inputFile = argument.inputFile!!
            listener.onStarted(argument.inputFile!!)
            delay(delayMillis)

            result = ProcessResult(
                resultCode = if (simulateSuccess) 0 else 1,
                output = listOf("Simulated ffmpeg output")
            )

            if (simulateSuccess) {
                listener.onCompleted(inputFile, argument.outputFile!!)
            } else {
                listener.onError(inputFile, "Simulated error")
            }
        }
    }


    @Test
    @DisplayName("Test FFmpeg Mock Success")
    fun scenarioSuccess() = runBlocking {
        val arguments = MpegArgument()
            .inputFile("input.mp4")
            .outputFile("output.mp4")
            .args(listOf("-y"))
            .withProgress(true)

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
            runner.run(arguments)
            assertTrue(listener.completed, "Expected onCompleted to be called")
        }

        assertTrue(elapsed >= 1000, "Expected to wait at least 1000 ms, but waited for $elapsed ms")
    }
}