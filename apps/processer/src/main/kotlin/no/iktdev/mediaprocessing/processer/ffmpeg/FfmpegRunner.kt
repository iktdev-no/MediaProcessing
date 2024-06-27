package no.iktdev.mediaprocessing.processer.ffmpeg

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.processer.ProcesserEnv
import no.iktdev.mediaprocessing.processer.ffmpeg.progress.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.processer.ffmpeg.progress.FfmpegProgressDecoder
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val log = KotlinLogging.logger {}
class FfmpegRunner(
    val inputFile: String,
    val outputFile: String,
    val arguments: List<String>,
    private val listener: FfmpegListener,
    val logDir: File

) {
    val currentDateTime = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH.mm")
    val formattedDateTime = currentDateTime.format(formatter)

    val logFile = logDir.using("$formattedDateTime-${File(inputFile).nameWithoutExtension}.log")

    val scope = CoroutineScope(Dispatchers.Unconfined + Job())
    private var job: Job? = null

    val decoder = FfmpegProgressDecoder()
    private val outputCache = mutableListOf<String>()

    fun isWorking(): Boolean {
        return job != null && (job?.isCompleted != true) && scope.isActive
    }

    fun run(progress: Boolean = false) {
        log.info { "Starting ffmpeg on file $inputFile" }
        val args = FfmpegArgumentsBuilder()
            .inputFile(inputFile)
            .outputFile(outputFile)
            .args(arguments)
            .allowOverwrite(ProcesserEnv.allowOverwrite)
            .withProgress(progress)
            .build()
        job = scope.launch {
            execute(args)
        }
    }

    fun isAlive(): Boolean {
        return scope.isActive && job?.isCompleted != true
    }

    private suspend fun execute(args: List<String>) {
        withContext(Dispatchers.IO) {
            logFile.createNewFile()
        }
        listener.onStarted(inputFile)
        val processOp = process(
            ProcesserEnv.ffmpeg, *args.toTypedArray(),
            stdout = Redirect.CAPTURE,
            stderr = Redirect.CAPTURE,
            consumer = {
                //log.info { it }
                onOutputChanged(it)
            },
            destroyForcibly = true
        )

        val result = processOp
        onOutputChanged("Received exit code: ${result.resultCode}")
        if (result.resultCode != 0) {
            listener.onError(inputFile, result.output.joinToString("\n"))
        } else {
            listener.onCompleted(inputFile, outputFile)
        }
    }

    fun cancel(message: String = "Work was interrupted as requested") {
        job?.cancel()
        scope.cancel(message)
        listener.onError(inputFile, message)
    }

    private var progress: FfmpegDecodedProgress? = null
    fun onOutputChanged(line: String) {
        outputCache.add(line)
        writeToLog(line)
        // toList is needed to prevent mutability.
        decoder.parseVideoProgress(outputCache.toList())?.let { decoded ->
            try {
                val _progress = decoder.getProgress(decoded)
                if (progress == null || _progress.progress > (progress?.progress ?: -1)) {
                    progress = _progress
                    listener.onProgressChanged(inputFile, _progress)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    fun writeToLog(line: String) {
        logFile.printWriter().use {
            it.appendLine(line)
        }
    }

}