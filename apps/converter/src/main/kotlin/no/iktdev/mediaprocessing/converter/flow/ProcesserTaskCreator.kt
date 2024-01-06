package no.iktdev.mediaprocessing.converter.flow

import no.iktdev.mediaprocessing.converter.ConverterCoordinator
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.common.tasks.TaskCreatorImpl
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess

abstract class ProcesserTaskCreator(coordinator: ConverterCoordinator):
    TaskCreatorImpl<ConverterCoordinator, PersistentProcessDataMessage, EventBasedProcessMessageListener>(coordinator) {

    override fun isPrerequisiteEventsOk(events: List<PersistentProcessDataMessage>): Boolean {
        val currentEvents = events.map { it.event }
        return requiredEvents.all { currentEvents.contains(it) }
    }

    override fun isPrerequisiteDataPresent(events: List<PersistentProcessDataMessage>): Boolean {
        val failed = events
            .filter { e -> e.event in requiredEvents }
            .filter { !it.data.isSuccess() }
        return failed.isEmpty()
    }

    override fun isEventOfSingle(event: PersistentProcessDataMessage, singleOne: KafkaEvents): Boolean {
        return event.event == singleOne
    }

}