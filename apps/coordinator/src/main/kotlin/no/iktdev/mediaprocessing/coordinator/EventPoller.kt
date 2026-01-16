package no.iktdev.mediaprocessing.coordinator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.iktdev.eventi.events.EventDispatcher
import no.iktdev.eventi.events.EventPollerImplementation
import no.iktdev.eventi.events.SequenceDispatchQueue
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component


@Component
class PollerAdministrator(
    private val eventPoller: EventPoller
) : SmartLifecycle {

    private var running = false

    var job: Job? = null
    override fun start() {
        job = CoroutineScope(Dispatchers.Default).launch {
            eventPoller.start()
        }
        running = true
    }

    override fun stop() {
        job?.cancel()
    }

    override fun isRunning() = running
}


val sequenceDispatcher = SequenceDispatchQueue(8)
val dispatcher = EventDispatcher(eventStore = EventStore)

@Component
@DependsOn("ExposedInit")
class EventPoller: EventPollerImplementation(eventStore = EventStore, dispatchQueue = sequenceDispatcher, dispatcher = dispatcher) {
}
