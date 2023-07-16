package no.iktdev.streamit.content.common.deamon

import com.github.pgreze.process.ProcessResult
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class Daemon(open val executable: String, val parameters: List<String>, val daemonInterface: IDaemon) {
    var executor: ProcessResult? = null
    suspend fun run(): Int {
        daemonInterface.onStarted()
        executor = process(executable, *parameters.toTypedArray(),
            stdout = Redirect.CAPTURE,
            stderr = Redirect.CAPTURE,
            consumer = {
                daemonInterface.onOutputChanged(it)
            })
        val resultCode = executor?.resultCode ?: -1
        if (resultCode == 0) {
            daemonInterface.onEnded()
        } else daemonInterface.onError()
        return resultCode
    }
}