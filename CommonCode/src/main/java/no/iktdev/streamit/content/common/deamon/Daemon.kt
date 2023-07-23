package no.iktdev.streamit.content.common.deamon

import com.github.pgreze.process.ProcessResult
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.google.gson.Gson
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

open class Daemon(open val executable: String, val daemonInterface: IDaemon) {
    var executor: ProcessResult? = null
    open suspend fun run(parameters: List<String>): Int {
        daemonInterface.onStarted()
        logger.info { "Daemon arguments: $executable ${Gson().toJson(parameters.toTypedArray())}"  }
        executor = process(executable, *parameters.toTypedArray(),
            stdout = Redirect.CAPTURE,
            stderr = Redirect.CAPTURE,
            consumer = {
                daemonInterface.onOutputChanged(it)
            })
        val resultCode = executor?.resultCode ?: -1
        if (resultCode == 0) {
            daemonInterface.onEnded()
        } else daemonInterface.onError(resultCode)
        return resultCode
    }
}