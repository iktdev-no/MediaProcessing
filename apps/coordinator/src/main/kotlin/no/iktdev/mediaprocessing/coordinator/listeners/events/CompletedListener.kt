package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreContentAndMetadataTaskResultEvent
import org.springframework.stereotype.Component

@Component
class CompletedListener: EventListener() {
    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        if (event !is StoreContentAndMetadataTaskResultEvent)
            return null
        return CompletedEvent().derivedOf(event)
    }
}