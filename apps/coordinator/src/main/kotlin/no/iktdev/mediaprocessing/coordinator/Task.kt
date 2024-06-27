package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.coordinator.coordination.PersistentEventBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.tasks.TaskCreatorImpl
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess

abstract class TaskCreator(coordinator: EventCoordinator):
    TaskCreatorImpl<EventCoordinator, PersistentMessage, PersistentEventBasedMessageListener>(coordinator) {



    override fun isPrerequisiteEventsOk(events: List<PersistentMessage>): Boolean {
        val currentEvents = events.map { it.event }
        return requiredEvents.all { currentEvents.contains(it) }
    }
    override fun isPrerequisiteDataPresent(events: List<PersistentMessage>): Boolean {
        val failed = events.filter { e -> e.event in requiredEvents }.filter { !it.data.isSuccess() }
        return failed.isEmpty()
    }

    override fun isEventOfSingle(event: PersistentMessage, singleOne: KafkaEvents): Boolean {
        return event.event == singleOne
    }

    /*override fun getListener(): Tasks {
        val eventListenerFilter = listensForEvents.ifEmpty { requiredEvents }
        return Tasks(taskHandler = this, producesEvent = producesEvent, listensForEvents = eventListenerFilter)
    }*/


    override fun prerequisitesRequired(events: List<PersistentMessage>): List<() -> Boolean> {
        return listOf {
            isPrerequisiteEventsOk(events)
        }
    }

    override fun prerequisiteRequired(event: PersistentMessage): List<() -> Boolean> {
        return listOf()
    }

    /**
     * Will always return null
     */
    open fun onProcessEventsAccepted(event: PersistentMessage, events: List<PersistentMessage>) {
        val referenceId = event.referenceId
        val eventIds = events.filter { it.event in requiredEvents + listensForEvents }.map { it.eventId }

        val current = processedEvents[referenceId] ?: setOf()
        current.toMutableSet().addAll(eventIds)
        processedEvents[referenceId] = current

        if (event.event == KafkaEvents.EventCollectAndStore) {
            processedEvents.remove(referenceId)
        }
    }

    override fun containsUnprocessedEvents(events: List<PersistentMessage>): Boolean {
        val referenceId = events.firstOrNull()?.referenceId ?:return false
        val preExistingEvents = processedEvents[referenceId]?: setOf()

        val forwardedEvents = events.filter { it.event in (requiredEvents + listensForEvents) }.map { it.eventId }
        val newEvents = forwardedEvents.filter { it !in preExistingEvents }
        return newEvents.isNotEmpty()

    }

}
