package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DetermineCollectionTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.DetermineCollectionTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component

@Component
class DetermineCollectionEventCreateListener : EventListener() {

    private val requiredEvents = listOf(
        MediaParsedInfoEvent::class.java,
        MetadataSearchResultEvent::class.java,
    )

    override fun onEvent(event: Event, history: List<Event>): Event? {

        // 1. Reager kun på events som faktisk er relevante
        if (!requiredEvents.any { it.isInstance(event) }) {
            return null
        }

        // 2. Sjekk om ALLE required events finnes i historikken
        if (!requiredEvents.all { type -> history.any { type.isInstance(it) } }) {
            return null
        }

        // 3. Hent kandidatnavn (projection-delen)
        val candidates = buildCollectionCandidates(history)

        // 4. Lag task
        val task = DetermineCollectionTask(names = candidates)
            .also { TaskStore.persist(it) }

        return DetermineCollectionTaskCreatedEvent(taskId = task.taskId)
    }

    private fun buildCollectionCandidates(history: List<Event>): List<String> {
        val candidates = mutableListOf<String>()

        history.getInstanceOf<MediaParsedInfoEvent>()
            ?.data?.parsedCollection
            ?.let { candidates.add(it) }

        history.getInstanceOf<MetadataSearchResultEvent>()
            ?.recommended?.metadata
            ?.let { metadata ->
                candidates.add(metadata.title)
                candidates.addAll(metadata.alternateTitles)
            }

        return candidates
    }
}
