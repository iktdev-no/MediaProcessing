package no.iktdev.mediaprocessing.processer

import no.iktdev.mediaprocessing.processer.coordination.PersistentEventProcessBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.common.tasks.TaskCreatorImpl
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess

abstract class TaskCreator(coordinator: Coordinator) :
    TaskCreatorImpl<Coordinator, PersistentProcessDataMessage, PersistentEventProcessBasedMessageListener>(coordinator) {

    override fun isPrerequisiteEventsOk(events: List<PersistentProcessDataMessage>): Boolean {
        val currentEvents = events.map { it.event }
        return requiredEvents.all { currentEvents.contains(it) }
    }
    override fun isPrerequisiteDataPresent(events: List<PersistentProcessDataMessage>): Boolean {
        val failed = events.filter { e -> e.event in requiredEvents }.filter { !it.data.isSuccess() }
        return failed.isEmpty()
    }

    override fun isEventOfSingle(event: PersistentProcessDataMessage, singleOne: KafkaEvents): Boolean {
        return event.event == singleOne
    }

    /*override fun getListener(): Tasks {
        val eventListenerFilter = listensForEvents.ifEmpty { requiredEvents }
        return Tasks(taskHandler = this, producesEvent = producesEvent, listensForEvents = eventListenerFilter)
    }*/


    override fun prerequisitesRequired(events: List<PersistentProcessDataMessage>): List<() -> Boolean> {
        return listOf {
            isPrerequisiteEventsOk(events)
        }
    }

    override fun prerequisiteRequired(event: PersistentProcessDataMessage): List<() -> Boolean> {
        return listOf()
    }

    override fun containsUnprocessedEvents(events: List<PersistentProcessDataMessage>): Boolean {
        return true
    }
}