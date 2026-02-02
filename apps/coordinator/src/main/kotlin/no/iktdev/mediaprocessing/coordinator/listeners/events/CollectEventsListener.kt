package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.listeners.SummaryEventListener
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.stereotype.Component

@Component
class CollectEventsListener(eventStore: no.iktdev.eventi.stores.EventStore = EventStore) : SummaryEventListener(eventStore) {

    private val log = KotlinLogging.logger {}
    override fun shouldSummarize(fullHistory: List<Event>): Boolean {
        val projection = CollectProjection(fullHistory)
        if (projection.startedWith == null) return false
        if (!projection.isWorkflowComplete()) return false
        return true
    }

    override fun produceSummary(fullHistory: List<Event>): Event {
        // Must have all relevant tasks completed
        val eventIds = fullHistory.map { it.eventId }.toSet()

        return CollectedEvent(eventIds).derivedOf(fullHistory.last())
    }

    override fun summaryAlreadyExists(fullHistory: List<Event>): Boolean {
        return fullHistory.any { it is CollectedEvent }
    }
}

