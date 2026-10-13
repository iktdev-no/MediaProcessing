package no.iktdev.mediaprocessing.shared.common.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import mu.KotlinLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

/** Application-owned background scope, cancelled when Spring shuts down. */
@Component
class ApplicationCoroutineScope : CoroutineScope, DisposableBean {
    private val log = KotlinLogging.logger {}

    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default +
        CoroutineExceptionHandler { _, exception ->
            if (exception !is CancellationException) {
                log.error(exception) { "Unhandled exception in application coroutine" }
            }
        }

    override fun destroy() {
        coroutineContext.cancel()
    }
}
