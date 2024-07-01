package no.iktdev.mediaprocessing.coordinator.utils

import no.iktdev.mediaprocessing.shared.common.persistance.*
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents


fun isAwaitingPrecondition(tasks: List<TaskType>, events: List<PersistentMessage>): Boolean {
    if (tasks.any { it == TaskType.Encode }) {
        if (events.lastOrNull { it.isOfEvent(
                KafkaEvents.EventMediaParameterEncodeCreated
            ) } == null) {
            return true
        }
    }


    if (tasks.any { it == TaskType.Convert } && tasks.none {it == TaskType.Extract}) {
        if (events.lastOrNull { it.isOfEvent(
                KafkaEvents.EventWorkConvertCreated
            ) } == null) {
            return true
        }
    } else if (tasks.any { it == TaskType.Convert }) {
        val extractEvent =  events.lastOrNull { it.isOfEvent(KafkaEvents.EventMediaParameterExtractCreated) }
        if (extractEvent == null || extractEvent.isSuccess()) {
            return true
        }
    }

    if (tasks.contains(TaskType.Extract)) {
        if (events.lastOrNull { it.isOfEvent(
                KafkaEvents.EventMediaParameterExtractCreated
            ) } == null) {
            return true
        }
    }


    return false
}


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

            val extractEvents = events.findLast { it.isOfEvent(KafkaEvents.EventMediaParameterExtractCreated) }
            if (extractEvents == null || extractEvents.isSkipped()) {
                false
            } else {
                val taskCreatedEvent = KafkaEvents.EventWorkConvertCreated
                val taskCompletedEvent = KafkaEvents.EventWorkConvertPerformed

                val argument = events.findLast { it.event == taskCreatedEvent } ?: return true
                if (!argument.isSuccess()) return false

                val trailingEvents = PersistentMessageHelper(events).getEventsRelatedTo(argument.eventId).filter {
                    it.event in listOf(
                        taskCreatedEvent,
                        taskCompletedEvent
                    )
                }
                trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size != trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size
            }
        }
    }
}