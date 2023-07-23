package no.iktdev.streamit.content.encode.runner

import mu.KotlinLogging
import no.iktdev.streamit.content.encode.EncodeEnv
import no.iktdev.exfl.observable.ObservableList
import no.iktdev.exfl.observable.observableListOf
import no.iktdev.streamit.content.common.deamon.Daemon
import no.iktdev.streamit.content.common.deamon.IDaemon
import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.content.encode.progress.Progress
import no.iktdev.streamit.content.encode.progress.ProgressDecoder
import java.io.File

private val logger = KotlinLogging.logger {}

class EncodeDaemon(val referenceId: String, val work: EncodeWork, val daemonInterface: IEncodeListener): IDaemon {
    var outputCache = observableListOf<String>()
    private val decoder = ProgressDecoder()
    private fun produceProgress(items: List<String>) {
        try {
            val progress = decoder.parseVideoProgress(items)
            if (progress != null) {
                daemonInterface.onProgress(referenceId, work, progress)
                outputCache.clear()
            }
        } catch (e: Exception) {
            e.message
        }
    }

    init {
        outputCache.addListener(object : ObservableList.Listener<String> {
            override fun onAdded(item: String) {
                produceProgress(outputCache)
            }
        })
    }

    suspend fun runUsingWorkItem(): Int {
        if (!File(work.outFile).parentFile.exists()) {
            File(work.outFile).parentFile.mkdirs()
        }
        val adjustedArgs = listOf(
            "-hide_banner", "-i", "'${work.inFile}'", *work.arguments.toTypedArray(), "'${work.outFile}'",
            "-progress", "pipe:1"
        ) + if (EncodeEnv.allowOverwrite) listOf("-y") else emptyList()
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
        outputCache.add(line)
        logger.info { "std -> $line" }
    }

}

interface IEncodeListener {
    fun onStarted(referenceId: String, work: EncodeWork)
    fun onError(referenceId: String, work: EncodeWork, code: Int)
    fun onProgress(referenceId: String, work: EncodeWork, progress: Progress)
    fun onEnded(referenceId: String, work: EncodeWork)
}