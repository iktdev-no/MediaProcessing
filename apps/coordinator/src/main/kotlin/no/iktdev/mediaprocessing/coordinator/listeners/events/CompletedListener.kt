package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreContentAndMetadataTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.getName
import org.springframework.stereotype.Component

@Component
class CompletedListener: EventListener() {
    val log = KotlinLogging.logger {}


    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        if (event !is StoreContentAndMetadataTaskResultEvent)
            return null
        val useEvent = event as StoreContentAndMetadataTaskResultEvent
        if (useEvent.status != TaskStatus.Completed) {
            log.info { "${useEvent.referenceId} - ${StoreContentAndMetadataTaskResultEvent::class.getName()} is failed, thus no task will be created" }
            return null
        }
        return CompletedEvent().derivedOf(event)
    }
}