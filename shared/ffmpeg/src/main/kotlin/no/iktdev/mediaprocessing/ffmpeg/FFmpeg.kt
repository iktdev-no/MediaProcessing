package no.iktdev.mediaprocessing.ffmpeg

import com.github.pgreze.process.ProcessResult
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.ffmpeg.arguments.MpegArgument
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegProgressDecoder
import no.iktdev.mediaprocessing.ffmpeg.util.UtcNow
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter

open class FFmpeg(val executable: String, val logDir: File) {
    open val listener: Listener? = null

    private var progress: FfmpegDecodedProgress? = null
    val decoder = FfmpegProgressDecoder()
    private val outputCache = mutableListOf<String>()

    //region Log File formatting
    val currentDateTime = UtcNow() // Instant, alltid UTC
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH.mm")
    val formattedDateTime = currentDateTime
        .atZone(ZoneId.systemDefault())
        .format(formatter)

    //endregion
    lateinit var logFile: File

    lateinit var result: ProcessResult
        protected set


    open fun onCreate() {}

    init {
        onCreate()
    }

    protected lateinit var inputFile: String
    open suspend fun run(argument: MpegArgument) {
        inputFile = if (argument.inputFile == null) throw RuntimeException("Input file is required") else  argument.inputFile!!
        logFile = logDir.using("$formattedDateTime-${File(inputFile).nameWithoutExtension}.log")
        listener?.onStarted(argument.inputFile!!)
        result = execute(argument.build()) {
            onNewOutput(it)
        }
        onNewOutput("Received exit code: ${result.resultCode}")
        if (result.resultCode != 0) {
            listener?.onError(inputFile, result.output.joinToString("\n"))
        } else {
            val success = moveAndVerify(argument)
            if (!success) {
                listener?.onError(inputFile, "Could not find output file at ${argument.outputFile}")
            } else {
                listener?.onCompleted(inputFile, argument.outputFile!!)
            }
        }
    }

    private fun moveAndVerify(argument: MpegArgument): Boolean {
        return if (argument.outputCacheFile) {
            File(argument.getOutputFileUsed()).renameTo(File(argument.outputFile!!))
        } else File(argument.outputFile!!).exists()
    }

    private suspend fun execute(arguments: List<String>, output: (String) -> Unit): ProcessResult {
        return process(executable, *arguments.toTypedArray(),
            stdout = Redirect.CAPTURE,
            stderr = Redirect.CAPTURE,
            consumer = {
                output(it)
            },
            destroyForcibly = true
        )
    }

    open fun onNewOutput(line: String) {
        outputCache.add(line)
        writeToLog(line)
        decoder.defineDuration(line)
        decoder.parseVideoProgress(outputCache.toList())?.let { decoded ->
            try {
                val _progress = decoder.getProgress(decoded)
                if (progress == null || _progress.progress > (progress?.progress ?: -1)) {
                    progress = _progress
                    listener?.onProgressChanged(inputFile, _progress)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    open fun writeToLog(line: String) {
        FileOutputStream(logFile, true).bufferedWriter(Charsets.UTF_8).use {
            it.appendLine(line)
        }
    }

    interface Listener {
        fun onStarted(inputFile: String)
        fun onCompleted(inputFile: String, outputFile: String)
        fun onProgressChanged(inputFile: String, progress: FfmpegDecodedProgress)
        fun onError(inputFile: String, message: String) {}
    }
}