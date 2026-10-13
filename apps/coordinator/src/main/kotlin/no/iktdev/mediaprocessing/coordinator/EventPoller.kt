package no.iktdev.mediaprocessing.coordinator

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.eventi.events.EventDispatcher
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.events.EventPollerImplementation
import no.iktdev.eventi.events.SequenceDispatchQueue
import no.iktdev.eventi.lifecycle.LifecycleStore
import no.iktdev.eventi.models.DispatchResult
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.common.coroutines.ApplicationCoroutineScope
import no.iktdev.mediaprocessing.shared.common.coroutines.runCancellablePollingLoop
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Duration

/* ---------------------------------------------------------
   CONFIG: Beans for dispatcher + queue
   --------------------------------------------------------- */

@Configuration
class EventiCoordinatorConfig {

    @Bean
    fun sequenceDispatchQueue(lifecycleStore: LifecycleStore): SequenceDispatchQueue =
        SequenceDispatchQueue(
            maxConcurrency = 8,
            lifecycleStore = lifecycleStore
        )

    @Bean
    fun overrideDispatcher(lifecycleStore: LifecycleStore): OverrideDispatcher =
        OverrideDispatcher(
            eventStore = EventStore,
            lifecycleStore = lifecycleStore
        )
}

/* ---------------------------------------------------------
   DISPATCHER OVERRIDE
   --------------------------------------------------------- */

class OverrideDispatcher(
    eventStore: EventStore,
    lifecycleStore: LifecycleStore
) : EventDispatcher(eventStore, lifecycleStore) {

    override fun onDispatched(
        event: Event,
        listener: EventListener,
        result: DispatchResult,
        message: String?
    ) {
        super.onDispatched(event, listener, result, message)
        // custom logic if needed
    }
}

/* ---------------------------------------------------------
   EVENT POLLER
   --------------------------------------------------------- */

@Component
@DependsOn("ExposedInit")
class EventPoller(
    private val lifecycleStore: LifecycleStore,
    private val sequenceDispatchQueue: SequenceDispatchQueue,
    private val overrideDispatcher: OverrideDispatcher
) : EventPollerImplementation(
    eventStore = EventStore,
    dispatchQueue = sequenceDispatchQueue,
    lifecycleStore = lifecycleStore,
    dispatcher = overrideDispatcher
) {
    private val log = KotlinLogging.logger {}

    override suspend fun start() {
        runCancellablePollingLoop(
            pollOnce = { pollOnce() },
            backoff = { backoff },
            increaseBackoff = {
                backoff = backoff.multipliedBy(2).coerceAtMost(Duration.ofMinutes(1))
            },
            onError = { error -> log.error(error) { "Error in poller loop" } },
            onCancellation = { cancelled ->
                log.debug { "Event poll loop cancelled: ${cancelled.message}" }
            }
        )
    }
}

/* ---------------------------------------------------------
   ADMINISTRATOR (STARTER POLLER)
   --------------------------------------------------------- */
@Profile("!noevent")
@Component
class EventPollerAdministrator(
    private val eventPoller: EventPoller,
    private val applicationCoroutineScope: ApplicationCoroutineScope
) : SmartLifecycle {

    private var running = false
    private var job: Job? = null

    override fun start() {
        job = applicationCoroutineScope.launch {
            eventPoller.start()
        }
        running = true
    }

    override fun stop() {
        job?.cancel()
        running = false
    }

    override fun isRunning(): Boolean = running
}
