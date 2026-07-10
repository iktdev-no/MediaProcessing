package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreMediaInfoAndMetadataTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.getName
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import org.springframework.stereotype.Component

@Component
class CompletedListener: EventListener() {
    val log = KotlinLogging.logger {}


    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event.requireQualifiedEntry<StoreMediaInfoAndMetadataTaskResultEvent>()
        if (useEvent.status != TaskStatus.Completed) {
            log.info { "${useEvent.referenceId} - ${StoreMediaInfoAndMetadataTaskResultEvent::class.getName()} is failed, thus no task will be created" }
            return null
        }
        return CompletedEvent().derivedOf(event)
    }
}