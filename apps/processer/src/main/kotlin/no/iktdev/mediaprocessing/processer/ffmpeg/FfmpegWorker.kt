package no.iktdev.mediaprocessing.processer.ffmpeg

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.processer.ProcesserEnv
import no.iktdev.mediaprocessing.processer.eventManager
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import java.io.File
import java.time.Duration

class FfmpegWorker(
    val referenceId: String,
    val eventId: String,
    val info: FfmpegWorkRequestCreated,
    val listener: FfmpegWorkerEvents,
    val logDir: File
) {
    private val scope = Coroutines.io()
    private var job: Job? = null

    fun isWorking(): Boolean {
        return job != null && (job?.isCompleted != true) && scope.isActive
    }

    val decoder = FfmpegProgressDecoder()
    private val outputCache = mutableListOf<String>()
    private val log = KotlinLogging.logger {}

    val logFile = logDir.using("$eventId-=-${File(info.outFile).nameWithoutExtension}.log")

    val getOutputCache = outputCache.toList()

    data class FfmpegWorkerArgumentsBuilder(
        private val mutableList: MutableList<String> = mutableListOf()
    ) {
        private val defaultArguments = listOf(
            "-nostdin",
            "-hide_banner"
        )
        private val progressArguments = listOf("-progress", "pipe:1")
        fun using(info: FfmpegWorkRequestCreated) = apply {
            this.mutableList.add(info.inputFile)
            this.mutableList.addAll(info.arguments)
            this.mutableList.add(info.outFile)
        }

        fun build(): List<String> {
            return (if (ProcesserEnv.allowOverwrite) listOf("-y") else emptyList()) + defaultArguments + listOf("-i") + mutableList
        }

        fun buildWithProgress(): List<String> {
            return build() + progressArguments
        }
    }

    fun run() {
        val args = FfmpegWorkerArgumentsBuilder().using(info).build()
        job = scope.launch {
            execute(args)
        }
    }

    fun runWithProgress() {
        log.info { "Starting ffmpeg ReferenceId: $referenceId, eventId $eventId for file ${info.outFile}" }
        val args = FfmpegWorkerArgumentsBuilder().using(info).buildWithProgress()
        job = scope.launch {
            execute(args)
        }
    }

    private suspend fun startIAmAlive() {
        scope.launch {
            while (scope.isActive && job?.isCompleted != true) {
                delay(Duration.ofMinutes(5).toMillis())
                listener.onIAmAlive(referenceId, eventId)
            }
        }
    }

    fun cancel(message: String = "Work was interrupted as requested") {
        job?.cancel()
        scope.cancel(message)
        listener.onError(referenceId, eventId, info, message)
    }


    private suspend fun execute(args: List<String>) {
        withContext(Dispatchers.IO) {
            logFile.createNewFile()
        }
        startIAmAlive()
        listener.onStarted(referenceId, eventId, info)
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
            listener.onError(referenceId, eventId, info, result.output.joinToString("\n"))
        } else {
            listener.onCompleted(referenceId, eventId, info)
        }
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
                    listener.onProgressChanged(referenceId, eventId, info, _progress)
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

interface FfmpegWorkerEvents {
    fun onStarted(referenceId: String, eventId: String, info: FfmpegWorkRequestCreated)
    fun onCompleted(referenceId: String, eventId: String, info: FfmpegWorkRequestCreated)
    fun onError(referenceId: String, eventId: String, info: FfmpegWorkRequestCreated, errorMessage: String)
    fun onProgressChanged(
        referenceId: String,
        eventId: String,
        info: FfmpegWorkRequestCreated,
        progress: FfmpegDecodedProgress
    )
    fun onIAmAlive(referenceId: String, eventId: String) {}
}