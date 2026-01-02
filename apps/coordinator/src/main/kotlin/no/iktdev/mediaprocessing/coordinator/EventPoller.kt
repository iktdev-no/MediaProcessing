package no.iktdev.mediaprocessing.coordinator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.iktdev.eventi.events.EventDispatcher
import no.iktdev.eventi.events.EventPollerImplementation
import no.iktdev.eventi.events.SequenceDispatchQueue
import no.iktdev.mediaprocessing.shared.common.stores.EventStore
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component


@Component
class PollerAdministrator(
    private val eventPoller: EventPoller,
): ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        CoroutineScope(Dispatchers.Default).launch {
            eventPoller.start()
        }
    }
}

val sequenceDispatcher = SequenceDispatchQueue(8)
val dispatcher = EventDispatcher(eventStore = EventStore)

@Component
class EventPoller: EventPollerImplementation(eventStore = EventStore, dispatchQueue = sequenceDispatcher, dispatcher = dispatcher) {
}
