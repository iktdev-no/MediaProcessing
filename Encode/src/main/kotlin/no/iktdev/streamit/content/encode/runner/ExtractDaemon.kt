package no.iktdev.streamit.content.encode.runner

import mu.KotlinLogging
import no.iktdev.streamit.content.encode.EncodeEnv
import no.iktdev.exfl.observable.observableListOf
import no.iktdev.streamit.content.common.deamon.Daemon
import no.iktdev.streamit.content.common.deamon.IDaemon
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import no.iktdev.streamit.content.encode.progress.Progress
import java.io.File
private val logger = KotlinLogging.logger {}

class ExtractDaemon(val referenceId: String, val work: ExtractWork, val daemonInterface: IExtractListener): IDaemon {
    var outputCache = observableListOf<String>()


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
    }

}

interface IExtractListener {
    fun onStarted(referenceId: String, work: ExtractWork)
    fun onError(referenceId: String, work: ExtractWork, code: Int)
    fun onProgress(referenceId: String, work: ExtractWork, progress: Progress) {}
    fun onEnded(referenceId: String, work: ExtractWork)
}