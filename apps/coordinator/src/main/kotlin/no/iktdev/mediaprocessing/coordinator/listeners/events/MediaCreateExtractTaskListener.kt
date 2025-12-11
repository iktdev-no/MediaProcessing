package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksExtractSelectedEvent
import org.springframework.stereotype.Component

@Component
class MediaCreateExtractTaskListener: EventListener() {
    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event as? MediaTracksExtractSelectedEvent ?: return null


        return null
    }
}