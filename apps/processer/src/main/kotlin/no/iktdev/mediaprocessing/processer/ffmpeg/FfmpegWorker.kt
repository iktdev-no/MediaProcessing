package no.iktdev.mediaprocessing.processer.ffmpeg

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.google.gson.Gson
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import no.iktdev.mediaprocessing.processer.ProcesserEnv
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class FfmpegWorker(val referenceId: String, val eventId: String, val info: FfmpegWorkRequestCreated, val listener: FfmpegWorkerEvents, val logDir: File) {
    val scope = Coroutines.io()
    val decoder = FfmpegProgressDecoder()
    private val outputCache = mutableListOf<String>()
    val logFile = logDir.using("$eventId-${File(info.outFile).nameWithoutExtension}.log")

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
        listener.onStarted(info)
        val processOp = process(
            ProcesserEnv.ffmpeg, *args.toTypedArray(),
            stdout = Redirect.CAPTURE,
            stderr = Redirect.CAPTURE,
            consumer = {
                onOutputChanged(it)
            },
            destroyForcibly = true)

        val result = processOp
        println(Gson().toJson(result))
        onOutputChanged("Received exit code: ${result.resultCode}")
        if (result.resultCode != 0) {
            listener.onError(info, result.output.joinToString("\n"))
        } else {
            listener.onCompleted(info)
        }
    }

    fun onOutputChanged(line: String) {
        outputCache.add(line)
        writeToLog(line)
        // toList is needed to prevent mutability.
        val progress = decoder.parseVideoProgress(outputCache.toList())
    }

    fun writeToLog(line: String) {
        logFile.printWriter().use {
            it.println(line)
        }
    }


}

interface FfmpegWorkerEvents {
    fun onStarted(info: FfmpegWorkRequestCreated,)
    fun onCompleted(info: FfmpegWorkRequestCreated)
    fun onError(info: FfmpegWorkRequestCreated, errorMessage: String)
    fun onProgressChanged(info: FfmpegWorkRequestCreated, progress: FfmpegDecodedProgress)
}