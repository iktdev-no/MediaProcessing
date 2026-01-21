package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.ListenerOrder
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoordinatorReadStreamsTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MediaReadTask
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.springframework.stereotype.Component

@ListenerOrder(3)
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
        ).derivedOf(event)

        TaskStore.persist(readTask)
        return CoordinatorReadStreamsTaskCreatedEvent(readTask.taskId).derivedOf(event) // Create task instead of event
    }
}