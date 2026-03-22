package no.iktdev.mediaprocessing.ffmpeg

import com.github.pgreze.process.ProcessResult
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.FfmpegDsl
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FFmpegTest: TestBase() {

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

    @Test
    @DisplayName("""
        Når ffmpeg bruker work file
        Hvis run() fullfører med exitcode 0
        Så:
            Skal moveAndVerify() kalles og output renames
        """)
    fun `ffmpeg renames work file when using work file`() = runTest {
        val logDir = workFolder.using("logs").apply { mkdirs() }

        // Ekte FFmpeg, men vi overstyrer execute() i en liten subclass
        val ffmpeg = object : FFmpeg("ffmpeg", logDir) {
            override suspend fun execute(arguments: List<String>, output: (String) -> Unit): ProcessResult {
                return ProcessResult(0, emptyList())
            }
        }

        // Lag DSL som bruker work file
        val dsl = ffmpeg {
            input("input.mp4") {
                video(0) {
                    map = true
                    codec = VideoCodec.Copy
                }
            }
            output("final.mkv") {
                overwrite = true
                progress = false
                useWorkFile = true
            }
            outputDirectory(workFolder)
        }

        // Nå må vi bruke DSL‑ens faktiske paths
        val workFile = IFile(dsl.outputWorkFile())
        val finalFile = IFile(dsl.outputFile())

        // Skriv innhold i work‑filen
        workFile.writeText("x")

        // Act
        ffmpeg.run(dsl)

        // Assert
        assertFalse(workFile.exists(), "Work file should be removed after rename")
        assertTrue(finalFile.exists(), "Final file should exist after rename")
        assertEquals("x", finalFile.readText())
    }




    @Test
    @DisplayName("""
    Når ffmpeg ikke bruker work file
    Hvis run() fullfører med exitcode 0
    Så:
        Skal moveAndVerify() IKKE kalles
        Og output skal skrives direkte til final file
""")
    fun `ffmpeg does not rename when not using work file`() = runTest {
        val logDir = workFolder.using("logs").apply { mkdirs() }

        // Ekte FFmpeg, men vi overstyrer execute() i en liten subclass
        val ffmpeg = object : FFmpeg("ffmpeg", logDir) {
            override suspend fun execute(arguments: List<String>, output: (String) -> Unit): ProcessResult {
                // Simulerer at ffmpeg skriver direkte til output-filen
                val out = IFile(arguments.last()) // siste argument er output path
                out.writeText("x")
                return ProcessResult(0, emptyList())
            }

            override fun moveAndVerify(command: FfmpegDsl): Boolean {
                error("moveAndVerify() skal IKKE kalles når useWorkFile = false")
            }
        }

        // DSL som IKKE bruker work file
        val dsl = ffmpeg {
            input("input.mp4") {
                video(0) {
                    map = true
                    codec = VideoCodec.Copy
                }
            }
            output("final.mkv") {
                overwrite = true
                progress = false
                useWorkFile = false   // <- viktig
            }
            outputDirectory(workFolder)
        }

        val workFile = IFile(dsl.outputWorkFile())   // dette skal IKKE brukes
        val finalFile = IFile(dsl.outputFile())

        // Skriv innhold i workFile for å verifisere at den ikke påvirkes
        workFile.writeText("x")

        // Act
        ffmpeg.run(dsl)

        // Assert
        assertTrue(workFile.exists(), "Work file should still exist when not using work file")
        assertTrue(finalFile.exists(), "Final file should exist when not using work file")
        assertEquals("x", finalFile.readText(), "Final file should contain the written output")
    }






}