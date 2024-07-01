package no.iktdev.mediaprocessing.coordinator.utils

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.persistance.*
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents

val log = KotlinLogging.logger {}

fun isAwaitingPrecondition(tasks: List<TaskType>, events: List<PersistentMessage>): Map<TaskType, Boolean> {
    val response = mutableMapOf<TaskType, Boolean>()
    if (tasks.any { it == TaskType.Encode }) {
        if (events.lastOrNull { it.isOfEvent(
                KafkaEvents.EventMediaParameterEncodeCreated
            ) } == null) {
            response[TaskType.Encode] = true
            log.info { "Waiting for ${KafkaEvents.EventMediaParameterEncodeCreated}" }

        }
    }

    val convertEvent = events.lastOrNull { it.isOfEvent(KafkaEvents.EventWorkConvertCreated) }
    if (tasks.any { it == TaskType.Convert } && tasks.none { it == TaskType.Extract }) {
        if (convertEvent == null) {
            response[TaskType.Convert] = true
            log.info { "Waiting for ${KafkaEvents.EventWorkConvertCreated}" }
        }
    } else if (tasks.any { it == TaskType.Convert }) {
        val extractEvent =  events.lastOrNull { it.isOfEvent(KafkaEvents.EventMediaParameterExtractCreated) }
        if (extractEvent == null || extractEvent.isSuccess() && convertEvent == null) {
            response[TaskType.Convert] = true
            log.info { "Waiting for ${KafkaEvents.EventMediaParameterExtractCreated}" }
        }
    }

    if (tasks.contains(TaskType.Extract)) {
        if (events.lastOrNull { it.isOfEvent(
                KafkaEvents.EventMediaParameterExtractCreated
            ) } == null) {
            response[TaskType.Extract] = true
            log.info { "Waiting for ${KafkaEvents.EventMediaParameterExtractCreated}" }

        }
    }


    return response
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