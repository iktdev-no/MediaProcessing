package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.ListenerOrder
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DetermineCollectionTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.DetermineCollectionTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.ofTypes
import no.iktdev.mediaprocessing.shared.common.requireEventValue
import no.iktdev.mediaprocessing.shared.common.takeIfCompleted
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@ListenerOrder(6)
@Component
class DetermineCollectionTaskCreateListener : EventListener() {

    override fun allowDerivativeOnHistoricalEvent() = true


    private fun getRequiredEvents(operations: Set<OperationType>): List<KClass<out Event>> {
        return if (operations.contains(OperationType.MetadataSearch)) {
            listOf(
                MediaParsedInfoEvent::class,
                MetadataSearchResultEvent::class,
            )
        } else listOf(MediaParsedInfoEvent::class)
    }

    override fun onEvent(event: Event, history: List<Event>): Event? {
        val useEvents = history + event
        val startOperations = useEvents.requireEventValue<StartProcessingEvent, Set<OperationType>> {it.data.operation}
        // Stop if completed
        if (useEvents.any { it is CompletedEvent }) return null

        // Avoid duplicates
        if (useEvents.any { it is DetermineCollectionTaskCreatedEvent }) return null

        val requiredEvents = getRequiredEvents(startOperations);
        // Check if all required events exist
        val hasAllRequired = requiredEvents.all { type ->
            useEvents.any { type.isInstance(it) }
        }
        if (!hasAllRequired) return null

        // Extract the required parent events
        val parents = useEvents.ofTypes(requiredEvents)

        // Build candidates
        val candidates = buildCollectionCandidates(useEvents)

        // Create task (use first parent for derivation)
        val task = DetermineCollectionTask(names = candidates)
            .derivedOf(event)   // <‑‑ IMPORTANT: derive from the triggering event only
        TaskStore.persist(task)

        // Create event (also derive from triggering event only)
        return DetermineCollectionTaskCreatedEvent(taskId = task.taskId)
            .derivedOf(event)
    }

    private fun buildCollectionCandidates(events: List<Event>): List<String> {
        val candidates = mutableListOf<String>()

        events.getInstanceOf<MediaParsedInfoEvent>()
            ?.data?.parsedCollection
            ?.let { candidates.add(it) }

        events.getInstanceOf<MetadataSearchResultEvent>()?.takeIfCompleted()
            ?.recommended?.metadata
            ?.let { metadata ->
                candidates.add(metadata.title)
                candidates.addAll(metadata.alternateTitles)
            }

        return candidates
    }
}
