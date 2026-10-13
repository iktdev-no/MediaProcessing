package no.iktdev.mediaprocessing.shared.common.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration

/** Runs a poll operation until its owning coroutine is cancelled. */
suspend fun runCancellablePollingLoop(
    pollOnce: suspend () -> Unit,
    backoff: () -> Duration,
    increaseBackoff: () -> Unit,
    onError: suspend (Exception) -> Unit,
    onCancellation: (CancellationException) -> Unit
) {
    while (currentCoroutineContext().isActive) {
        try {
            pollOnce()
        } catch (cancelled: CancellationException) {
            onCancellation(cancelled)
            return
        } catch (error: Exception) {
            onError(error)
            delay(backoff().toMillis())
            increaseBackoff()
        }
    }
}
