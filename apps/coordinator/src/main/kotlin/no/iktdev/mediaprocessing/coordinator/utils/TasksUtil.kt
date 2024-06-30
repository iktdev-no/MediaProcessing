package no.iktdev.mediaprocessing.coordinator.utils

import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessageHelper
import no.iktdev.mediaprocessing.shared.common.persistance.isOfEvent
import no.iktdev.mediaprocessing.shared.common.persistance.isSuccess
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents

fun isAwaitingTask(task: TaskType, events: List<PersistentMessage>): Boolean {
    return when (task) {
        TaskType.Encode -> {
            val argumentEvent = KafkaEvents.EventMediaParameterEncodeCreated
            val taskCreatedEvent = KafkaEvents.EventWorkEncodeCreated
            val taskCompletedEvent = KafkaEvents.EventWorkEncodePerformed

            val argument = events.findLast { it.event == argumentEvent } ?: return true
            if (!argument.isSuccess()) return false

            val trailingEvents = PersistentMessageHelper(events).getEventsRelatedTo(argument.eventId).filter {
                it.event in listOf(
                    argumentEvent,
                    taskCreatedEvent,
                    taskCompletedEvent
                )
            }

            trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size != trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size


        }
        TaskType.Extract -> {
            val argumentEvent = KafkaEvents.EventMediaParameterExtractCreated
            val taskCreatedEvent = KafkaEvents.EventWorkExtractCreated
            val taskCompletedEvent = KafkaEvents.EventWorkExtractPerformed

            val argument = events.findLast { it.event == argumentEvent } ?: return true
            if (!argument.isSuccess()) return false
            val trailingEvents = PersistentMessageHelper(events).getEventsRelatedTo(argument.eventId).filter {
                it.event in listOf(
                    argumentEvent,
                    taskCreatedEvent,
                    taskCompletedEvent
                )
            }
            trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size != trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size
        }
        TaskType.Convert -> {
            val argumentEvent = KafkaEvents.EventMediaParameterConvertCreated
            val taskCreatedEvent = KafkaEvents.EventWorkConvertCreated
            val taskCompletedEvent = KafkaEvents.EventWorkConvertPerformed

            val argument = events.findLast { it.event == argumentEvent } ?: return true
            if (!argument.isSuccess()) return false

            val trailingEvents = PersistentMessageHelper(events).getEventsRelatedTo(argument.eventId).filter {
                it.event in listOf(
                    argumentEvent,
                    taskCreatedEvent,
                    taskCompletedEvent
                )
            }
            trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size != trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size

        }
    }
}