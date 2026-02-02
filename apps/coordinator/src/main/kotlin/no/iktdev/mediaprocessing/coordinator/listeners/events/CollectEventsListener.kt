package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import org.springframework.stereotype.Component

@Component
class CollectEventsListener : EventListener() {
    private val log = KotlinLogging.logger {}

    override fun onEvent(event: Event, history: List<Event>): Event? {
        // Avoid double-collection
        if (event is CollectedEvent || history.any { it is CollectedEvent }) return null

        val projection = CollectProjection(history)

        // Must have a StartProcessingEvent
        if (projection.startedWith == null) return null
        

        // Must have all relevant tasks completed
        if (!projection.isWorkflowComplete()) return null

        return CollectedEvent(history.map { it.eventId }.toSet()).derivedOf(event)
    }
}

