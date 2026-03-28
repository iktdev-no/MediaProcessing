package no.iktdev.mediaprocessing.ffmpeg

import com.github.pgreze.process.ProcessResult
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import mu.KotlinLogging
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegProgressDecoder
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.FfmpegDsl
import no.iktdev.mediaprocessing.ffmpeg.util.UtcNow
import org.jetbrains.annotations.VisibleForTesting

import java.time.ZoneId
import java.time.format.DateTimeFormatter

open class FFmpeg(val executable: String, val logDir: IFile) {
    private val log = KotlinLogging.logger {}

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
    lateinit var logFile: IFile

    lateinit var result: ProcessResult
        protected set


    open fun onCreate() {}

    protected lateinit var inputFile: String
    open suspend fun run(command: FfmpegDsl, onPid: (Long) -> Unit = {}) {
        inputFile = command.toInstructions().findPrimaryInput()
        logFile = logDir.using("$formattedDateTime-${IFile(inputFile).nameWithoutExtension}.log")
        listener?.onStarted(inputFile)
        val arguments = command.build()
        log.debug("Running ffmpeg with the following arguments\n${arguments.joinToString(" ")}")
        result = execute(arguments, pid = onPid) {
            onNewOutput(it)
        }
        onNewOutput("Received exit code: ${result.resultCode}")
        if (result.resultCode != 0) {
            listener?.onError(inputFile, result.output.joinToString("\n"))
            log.error { "Exitcode was ${result.resultCode}, ffmpeg was attempted with the following arguments: $arguments" }
            log.info { "Log file can be found at ${logFile.absolutePath}" }
        } else {

            if (command.isUsingWorkFile()) {
                val success = moveAndVerify(command)
                if (!success) {
                    listener?.onError(inputFile, "Could not find output file at ${command.outputWorkFile()}")
                }
            }
            listener?.onCompleted(inputFile, command.outputFile())
        }
    }

    open fun moveAndVerify(command: FfmpegDsl): Boolean {
        return if (command.isUsingWorkFile()) {
            IFile(command.outputWorkFile()).renameTo(IFile(command.outputFile()))
        } else {
            true
        }
    }

    @VisibleForTesting
    internal open suspend fun execute(arguments: List<String>, pid: (Long) -> Unit, output: (String) -> Unit): ProcessResult {
        return process(executable, *arguments.toTypedArray(),
            stdout = Redirect.CAPTURE,
            stderr = Redirect.CAPTURE,
            consumer = {
                output(it)
            },
            destroyForcibly = true,
            onProcessStarted = { pid ->
                pid?.let { pid(it) } ?: run {
                    log.warn("Unable to obtain pid on start")
                }
            }
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
        logFile.appendLine(line)
    }

    interface Listener {
        fun onStarted(inputFile: String)
        fun onCompleted(inputFile: String, outputFile: String)
        fun onProgressChanged(inputFile: String, progress: FfmpegDecodedProgress)
        fun onError(inputFile: String, message: String) {}
    }
}