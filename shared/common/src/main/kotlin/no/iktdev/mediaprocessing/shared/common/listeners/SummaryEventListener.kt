package no.iktdev.mediaprocessing.shared.common.listeners

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.eventi.stores.EventStore
import no.iktdev.mediaprocessing.shared.common.effectivePersisted

abstract class SummaryEventListener(
    private val eventStore: EventStore
) : EventListener() {

    final override fun onEvent(event: Event, history: List<Event>): Event? {
        val fullHistory = eventStore.getPersistedEventsFor(event.referenceId)
            .effectivePersisted()
        val events = fullHistory.map { it.toEvent() }.filterNotNull()

        if (!shouldSummarize(events)) return null
        if (summaryAlreadyExists(events)) return null

        return produceSummary(events).derivedOf(event)
    }

    abstract fun shouldSummarize(fullHistory: List<Event>): Boolean
    abstract fun produceSummary(fullHistory: List<Event>): Event
    abstract fun summaryAlreadyExists(fullHistory: List<Event>): Boolean

}
