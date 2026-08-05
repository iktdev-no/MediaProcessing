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
        val effectiveHistory = eventStore.getPersistedEventsFor(event.referenceId)
            .effectivePersisted()
        val events = effectiveHistory.mapNotNull { it.toEvent() }

        if (!shouldSummarize(events)) return null
        if (summaryAlreadyExists(events)) return null

        return produceSummary(events).derivedOf(event)
    }

    abstract fun shouldSummarize(effectiveHistory: List<Event>): Boolean
    abstract fun produceSummary(effectiveHistory: List<Event>): Event
    abstract fun summaryAlreadyExists(effectiveHistory: List<Event>): Boolean

}
