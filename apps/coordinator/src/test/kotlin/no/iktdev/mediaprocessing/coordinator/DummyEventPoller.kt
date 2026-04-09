package no.iktdev.mediaprocessing.coordinator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@Profile("testDev")
class DummyEventPoller(
    private val eventStore: EventStore
) : SmartLifecycle {

    private var running = false
    private var job: Job? = null

    private var scanFrom: Instant = Instant.EPOCH

    override fun start() {
        job = CoroutineScope(Dispatchers.Default).launch {
            runDummyPoller()
        }
        running = true
    }

    override fun stop() {
        job?.cancel()
        running = false
    }

    override fun isRunning(): Boolean = running

    private suspend fun runDummyPoller() {
        println("=== Dummy Poller Started ===")
        println("Initial scanFrom = $scanFrom")

        while (currentCoroutineContext().isActive) {
            val events = eventStore.getPersistedEventsAfter(scanFrom)

            println("\n--- POLL ---")
            println("scanFrom = $scanFrom")
            println("found    = ${events.size}")

            if (events.isEmpty()) {
                println("No events → bumping scanFrom by 1ns")
                scanFrom = scanFrom.plusNanos(1)
                delay(1000)
                continue
            }

            events.forEach { ev ->
                println("Event ${ev.id}")
                println("  persistedAt = ${ev.persistedAt}")
                println("  referenceId = ${ev.referenceId}")
            }

            val maxTs = events.maxOf { it.persistedAt }

            when {
                maxTs > scanFrom -> {
                    println("→ bump scanFrom to $maxTs")
                    scanFrom = maxTs
                }
                maxTs == scanFrom -> {
                    println("→ livelock detected: bumping scanFrom by 1ns")
                    scanFrom = scanFrom.plusNanos(1)
                }
                maxTs < scanFrom -> {
                    println("🔥 ERROR: DB returned event older than scanFrom!")
                    println("scanFrom = $scanFrom")
                    println("maxTs    = $maxTs")
                }
            }

            delay(1000)
        }
    }


}