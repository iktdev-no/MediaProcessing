package no.iktdev.streamit.content.common.deamon

import com.github.pgreze.process.ProcessResult
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.google.gson.Gson
import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines

private val logger = KotlinLogging.logger {}

open class Daemon(open val executable: String, val daemonInterface: IDaemon) {
    val scope = Coroutines.io()
    var job: Job? = null
    var executor: ProcessResult? = null
    open suspend fun run(parameters: List<String>): Int {
        daemonInterface.onStarted()
        logger.info { "\nDaemon arguments: $executable \nParamters:\n${parameters.joinToString(" ")}"  }
        job = scope.launch {
            executor = process(executable, *parameters.toTypedArray(),
                stdout = Redirect.CAPTURE,
                stderr = Redirect.CAPTURE,
                consumer = {
                    daemonInterface.onOutputChanged(it)
                })
        }
        job?.join()

        val resultCode = executor?.resultCode ?: -1
        if (resultCode == 0) {
            daemonInterface.onEnded()
        } else daemonInterface.onError(resultCode)
        logger.info { "$executable result: $resultCode" }
        return resultCode
    }

    suspend fun cancel() {
        job?.cancelAndJoin()
        scope.cancel("Cancel operation triggered!")
    }
}