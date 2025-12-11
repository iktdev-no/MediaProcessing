package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import org.springframework.stereotype.Component

@Component
class MediaCreateConvertTaskListener: EventListener() {
    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        return null;
    }
}