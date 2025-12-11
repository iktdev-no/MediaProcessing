package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamReadTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MediaReadTask
import no.iktdev.mediaprocessing.shared.common.stores.TaskStore
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Order(3)
@Component
class MediaReadStreamsTaskCreatedListener: EventListener() {
    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        if (event !is MediaParsedInfoEvent) return null
        val startEvent = history.lastOrNull { it is StartProcessingEvent } as? StartProcessingEvent
            ?: return null


        val readTask = MediaReadTask(
            fileUri = startEvent.data.fileUri
        )

        TaskStore.persist(readTask)
        return null // Create task instead of event
    }
}