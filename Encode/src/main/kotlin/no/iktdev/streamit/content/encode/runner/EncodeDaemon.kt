package no.iktdev.streamit.content.encode.runner

import mu.KotlinLogging
import no.iktdev.streamit.content.encode.EncodeEnv
import no.iktdev.exfl.observable.ObservableList
import no.iktdev.exfl.observable.observableListOf
import no.iktdev.streamit.content.common.deamon.Daemon
import no.iktdev.streamit.content.common.deamon.IDaemon
import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.content.encode.progress.DecodedProgressData
import no.iktdev.streamit.content.encode.progress.Progress
import no.iktdev.streamit.content.encode.progress.ProgressDecoder
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

private val logger = KotlinLogging.logger {}

class EncodeDaemon(val referenceId: String, val work: EncodeWork, val daemonInterface: IEncodeListener): IDaemon {
    val logDir = File("/src/logs")
    lateinit var outLogFile: File
    var outputCache = observableListOf<String>()
    private val decoder = ProgressDecoder(work)
    fun produceProgress(items: List<String>): Progress? {
        try {
            val decodedProgress = decoder.parseVideoProgress(items)
            if (decodedProgress != null) {
                val progress = decoder.getProgress(decodedProgress)
                outputCache.clear()
                return progress
            }
        } catch (e: IndexOutOfBoundsException) {
            // Do nothing
        } catch (e: Exception) {
            //logger.error { e.message }
            e.printStackTrace()
        }
        return null
    }

    init {
        outputCache.addListener(object : ObservableList.Listener<String> {
            override fun onAdded(item: String) {
                val progress = produceProgress(outputCache)
                progress?.let {
                    daemonInterface.onProgress(referenceId, work, progress)
                }
            }
        })
        logDir.mkdirs()
        outLogFile = File(logDir, "${work.workId}-${work.collection}.log")
    }

    suspend fun runUsingWorkItem(): Int {
        val outFile = File(work.outFile)
        if (!outFile.parentFile.exists()) {
            outFile.parentFile.mkdirs()
        }
        val adjustedArgs = listOf(
            "-hide_banner", "-i", File(work.inFile).absolutePath, *work.arguments.toTypedArray(), outFile.absolutePath,
            "-progress", "pipe:1"
        ) + if (EncodeEnv.allowOverwrite) listOf("-y") else listOf("-nostdin")
        logger.info { "$referenceId @ ${work.workId} ${adjustedArgs.joinToString(" ")}" }
        return Daemon(EncodeEnv.ffmpeg, this).run(adjustedArgs)
    }

    override fun onStarted() {
        super.onStarted()
        daemonInterface.onStarted(referenceId, work)
    }

    override fun onEnded() {
        super.onEnded()
        daemonInterface.onEnded(referenceId, work)
    }

    override fun onError(code: Int) {
        daemonInterface.onError(referenceId, work, code)
    }

    override fun onOutputChanged(line: String) {
        super.onOutputChanged(line)
        if (decoder.isDuration(line))
            decoder.setDuration(line)
        if (decoder.expectedKeys.any { line.startsWith(it) }) {
            outputCache.add(line)
        }
        writeToLog(line)
    }
    private fun writeToLog(line: String) {
        val fileWriter = FileWriter(outLogFile, true) // true indikerer at vi ønsker å appende til filen
        val bufferedWriter = BufferedWriter(fileWriter)

        // Skriv logglinjen til filen
        bufferedWriter.write(line)
        bufferedWriter.newLine() // Legg til en ny linje etter logglinjen

        // Lukk BufferedWriter og FileWriter for å frigjøre ressurser
        bufferedWriter.close()
        fileWriter.close()
    }

}

interface IEncodeListener {
    fun onStarted(referenceId: String, work: EncodeWork)
    fun onError(referenceId: String, work: EncodeWork, code: Int)
    fun onProgress(referenceId: String, work: EncodeWork, progress: Progress)
    fun onEnded(referenceId: String, work: EncodeWork)
}