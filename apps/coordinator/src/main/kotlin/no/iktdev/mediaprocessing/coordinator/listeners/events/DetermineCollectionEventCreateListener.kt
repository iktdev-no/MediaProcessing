package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.ListenerOrder
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DetermineCollectionTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.DetermineCollectionTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component

@ListenerOrder(6)
@Component
class DetermineCollectionEventCreateListener : EventListener() {

    private val requiredEvents = listOf(
        MediaParsedInfoEvent::class.java,
        MetadataSearchResultEvent::class.java,
    )

    override fun onEvent(event: Event, history: List<Event>): Event? {

        // 1. Kombiner history + event slik at vi får "faktisk state"
        val useEvents = history + event

        // 2. Sjekk om ALLE required events finnes i useEvents
        val hasAllRequired = requiredEvents.all { type ->
            useEvents.any { type.isInstance(it) }
        }

        if (!hasAllRequired) {
            return null
        }

        // 3. Unngå duplikater
        if (useEvents.any { it is DetermineCollectionTaskCreatedEvent }) {
            return null
        }

        // 4. Bygg kandidatnavn
        val candidates = buildCollectionCandidates(useEvents)

        // 5. Lag task
        val task = DetermineCollectionTask(names = candidates)
        TaskStore.persist(task)

        // 6. Returner eventet korrekt koblet
        return DetermineCollectionTaskCreatedEvent(taskId = task.taskId)
            .derivedOf(event)
    }

    private fun buildCollectionCandidates(events: List<Event>): List<String> {
        val candidates = mutableListOf<String>()

        events.getInstanceOf<MediaParsedInfoEvent>()
            ?.data?.parsedCollection
            ?.let { candidates.add(it) }

        events.getInstanceOf<MetadataSearchResultEvent>()
            ?.recommended?.metadata
            ?.let { metadata ->
                candidates.add(metadata.title)
                candidates.addAll(metadata.alternateTitles)
            }

        return candidates
    }
}
