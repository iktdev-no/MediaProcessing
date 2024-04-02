package no.iktdev.mediaprocessing.processer.ffmpeg

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.processer.ProcesserEnv
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import java.io.File

class FfmpegWorker(val referenceId: String, val eventId: String, val info: FfmpegWorkRequestCreated, val listener: FfmpegWorkerEvents, val logDir: File) {
    val scope = Coroutines.io()
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

    suspend fun run() {
        val args = FfmpegWorkerArgumentsBuilder().using(info).build()
        execute(args)
    }

    suspend fun runWithProgress() {
        val args = FfmpegWorkerArgumentsBuilder().using(info).buildWithProgress()
        execute(args)
    }

    private suspend fun execute(args: List<String>) {
        withContext(Dispatchers.IO) {
            logFile.createNewFile()
        }
        listener.onStarted(referenceId, eventId, info)
        val processOp = process(
            ProcesserEnv.ffmpeg, *args.toTypedArray(),
            stdout = Redirect.CAPTURE,
            stderr = Redirect.CAPTURE,
            consumer = {
                //log.info { it }
                onOutputChanged(it)
            },
            destroyForcibly = true)

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
                if (progress == null || _progress.progress > (progress?.progress ?: -1) ) {
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
    fun onStarted(referenceId: String, eventId: String, info: FfmpegWorkRequestCreated,)
    fun onCompleted(referenceId: String, eventId: String, info: FfmpegWorkRequestCreated)
    fun onError(referenceId: String, eventId: String, info: FfmpegWorkRequestCreated, errorMessage: String)
    fun onProgressChanged(referenceId: String, eventId: String, info: FfmpegWorkRequestCreated, progress: FfmpegDecodedProgress)
}