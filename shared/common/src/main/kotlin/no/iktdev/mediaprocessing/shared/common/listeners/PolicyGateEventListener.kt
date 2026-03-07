package no.iktdev.mediaprocessing.shared.common.listeners

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.SignalEvent
import no.iktdev.eventi.stores.EventStore
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.mediaprocessing.shared.common.effectivePersisted
import no.iktdev.mediaprocessing.shared.common.getInstancesOf
import java.util.UUID

abstract class PolicyGateEventListener(
    private val eventStore: EventStore
) : EventListener() {

    override fun onEvent(event: Event, history: List<Event>): Event? {
        val fullHistory = eventStore.getPersistedEventsFor(event.referenceId)
            .map { it.toEvent() }
        val signalHistory = fullHistory.filterIsInstance<SignalEvent>()

        if (hasPassed(history)) {
            return null
        }

        if (!isRequiredPrecursorEventPresent(history)) {
            return null
        }

        if (isOnHold(history.getInstancesOf<SignalEvent>())) {
            return null
        }

        return handlePolicy(event, history, signalHistory)
    }

    fun getSignals(referenceId: UUID): List<SignalEvent> {
        return eventStore.getPersistedEventsFor(referenceId)
            .effectivePersisted()
            .mapNotNull { it.toEvent() }
            .filterIsInstance<SignalEvent>()
    }

    abstract fun isRequiredPrecursorEventPresent(history: List<Event>): Boolean
    abstract fun isOnHold(signalHistory: List<SignalEvent>): Boolean
    abstract fun hasPassed(history: List<Event>): Boolean

    abstract fun handlePolicy(event: Event, history: List<Event>, signalHistory: List<SignalEvent>): Event?
}

