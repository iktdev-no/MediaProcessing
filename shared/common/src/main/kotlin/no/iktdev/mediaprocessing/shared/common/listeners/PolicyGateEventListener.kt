package no.iktdev.mediaprocessing.shared.common.listeners

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.SignalEvent
import no.iktdev.eventi.stores.EventStore
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.mediaprocessing.shared.common.effectivePersisted

abstract class PolicyGateEventListener(
    private val eventStore: EventStore
) : EventListener() {

    override fun onEvent(event: Event, history: List<Event>): Event? {
        val fullHistory = eventStore.getPersistedEventsFor(event.referenceId)
            .effectivePersisted()
            .mapNotNull { it.toEvent() }

        val signalHistory = fullHistory.filterIsInstance<SignalEvent>()

        if (!isRequiredPrecursorEventPresent(fullHistory)) {
            return null
        }

        if (isOnHold(signalHistory)) {
            return handleOnHold(event, fullHistory, signalHistory)
        }

        if (allowPassthrough(event, fullHistory, signalHistory)) {
            return producePassthroughEvent(event, fullHistory, signalHistory)
        }

        return handlePolicy(event, fullHistory, signalHistory)
    }

    abstract fun isRequiredPrecursorEventPresent(fullHistory: List<Event>): Boolean
    abstract fun isOnHold(signalHistory: List<SignalEvent>): Boolean
    abstract fun handleOnHold(event: Event, fullHistory: List<Event>, signalHistory: List<SignalEvent>): Event?
    abstract fun allowPassthrough(event: Event, fullHistory: List<Event>, signalHistory: List<SignalEvent>): Boolean
    abstract fun handlePolicy(event: Event, fullHistory: List<Event>, signalHistory: List<SignalEvent>): Event?
    abstract fun producePassthroughEvent(event: Event, fullHistory: List<Event>, signalHistory: List<SignalEvent>): Event?
}

