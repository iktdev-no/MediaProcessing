package no.iktdev.mediaprocessing.coordinator

import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.coordination.PersistentEventBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.tasks.TaskCreatorImpl
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess

abstract class TaskCreator(coordinator: Coordinator):
    TaskCreatorImpl<Coordinator, PersistentMessage, PersistentEventBasedMessageListener>(coordinator) {
    val log = KotlinLogging.logger {}


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

}
