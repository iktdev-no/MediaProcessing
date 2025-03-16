package no.iktdev.mediaprocessing.shared.common

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class LogKey(
    val referenceId: String,
    val listener: String,
    val eventType: String
)

object EventDeadlockDetector {

    private val stuckEvents = ConcurrentHashMap<LogKey, Int>()
    private val suppressedEvents = ConcurrentHashMap<LogKey, Boolean>()
    private val threshold = 5 // Antall ganger før det anses som deadlock
    private val resetInterval = 10L // Reset telling hvert 10. sekund

    init {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            stuckEvents.clear()
        }, resetInterval, resetInterval, TimeUnit.SECONDS)
    }

    fun detect(referenceId: String, listener: String, eventType: String): Boolean {
        val key = LogKey(referenceId, listener, eventType)

        if (suppressedEvents[key] == true) {
            return false
        }

        val count = stuckEvents.merge(key, 1) { old, _ -> old + 1 } ?: 1

        if (count > threshold) {
            suppressedEvents[key] = true
            onDeadlockDetected(key)
            return false
        }

        return true
    }

    fun resolve(referenceId: String, listener: String, eventType: String) {
        val key = LogKey(referenceId, listener, eventType)
        stuckEvents.remove(key)
        suppressedEvents.remove(key)
    }

    private fun onDeadlockDetected(key: LogKey) {
        println("🚨 Deadlock detected! ReferenceId=${key.referenceId}, Listener=${key.listener}, EventType=${key.eventType}")
        // Her kan du f.eks. sende et varsel, restarte prosess, etc.
    }
}
