package no.iktdev.mediaprocessing.processer.listeners

import com.github.pgreze.process.ProcessResult
import kotlinx.coroutines.delay
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.FfmpegDsl

class MockFFmpeg(override val listener: Listener, val delayMillis: Long = 500, private val simulateSuccess: Boolean = true) : FFmpeg(executable = "", logDir = IFile("/null")) {

    companion object {
        fun emptyListener() = object : Listener {
            override fun onStarted(inputFile: String) {}
            override fun onCompleted(inputFile: String, outputFile: String) {}
            override fun onProgressChanged(inputFile: String, progress: FfmpegDecodedProgress) {}
            override fun onError(inputFile: String, message: String) {}
        }
    }

    override suspend fun run(command: FfmpegDsl, onPid: (Long) -> Unit) {
        logFile = IFile("build/test-log/file.json")
        inputFile = command.toInstructions().findPrimaryInput()
        listener.onStarted(inputFile)
        delay(delayMillis)

        result = ProcessResult(
            resultCode = if (simulateSuccess) 0 else 1,
            output = listOf("Simulated ffmpeg output")
        )

        if (simulateSuccess) {
            listener.onCompleted(inputFile, command.outputFile())
        } else {
            listener.onError(inputFile, "Simulated error")
        }
    }

}