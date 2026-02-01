package no.iktdev.mediaprocessing.processer.listeners

import com.github.pgreze.process.ProcessResult
import kotlinx.coroutines.delay
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.arguments.MpegArgument
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import java.io.File

class MockFFmpeg(override val listener: Listener, val delayMillis: Long = 500, private val simulateSuccess: Boolean = true) : FFmpeg(executable = "", logDir = File("/null")) {

    companion object {
        fun emptyListener() = object : Listener {
            override fun onStarted(inputFile: String) {}
            override fun onCompleted(inputFile: String, outputFile: String) {}
            override fun onProgressChanged(inputFile: String, progress: FfmpegDecodedProgress) {}
            override fun onError(inputFile: String, message: String) {}
        }
    }

    override suspend fun run(argument: MpegArgument) {
        logFile = File("build/test-log/file.json")
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