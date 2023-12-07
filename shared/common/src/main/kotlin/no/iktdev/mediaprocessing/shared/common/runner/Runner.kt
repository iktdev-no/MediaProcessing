package no.iktdev.mediaprocessing.shared.common.runner

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines

open class Runner(open val executable: String, val daemonInterface: IRunner) {
    private val logger = KotlinLogging.logger {}

    val scope = Coroutines.io()
    var job: Job? = null
    var executor: com.github.pgreze.process.ProcessResult? = null
    open suspend fun run(parameters: List<String>): Int {
        daemonInterface.onStarted()
        logger.info { "\nDaemon arguments: $executable \nParamters:\n${parameters.joinToString(" ")}"  }
        job = scope.launch {
            executor = com.github.pgreze.process.process(executable, *parameters.toTypedArray(),
                stdout = com.github.pgreze.process.Redirect.CAPTURE,
                stderr = com.github.pgreze.process.Redirect.CAPTURE,
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