package no.iktdev.mediaprocessing.coordinator.utils

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.common.contract.data.Event

val log = KotlinLogging.logger {}

/*
fun isAwaitingPrecondition(tasks: List<TaskType>, events: List<Event>): Map<TaskType, Boolean> {
    val response = mutableMapOf<TaskType, Boolean>()
    if (tasks.any { it == TaskType.Encode }) {
        if (events.lastOrNull { it.isOfEvent(
                Events.EventMediaParameterEncodeCreated
            ) } == null) {
            response[TaskType.Encode] = true
            log.info { "Waiting for ${Events.EventMediaParameterEncodeCreated}" }

        }
    }

    val convertEvent = events.lastOrNull { it.isOfEvent(Events.EventWorkConvertCreated) }
    if (tasks.any { it == TaskType.Convert } && tasks.none { it == TaskType.Extract }) {
        if (convertEvent == null) {
            response[TaskType.Convert] = true
            log.info { "Waiting for ${Events.EventWorkConvertCreated}" }
        }
    } else if (tasks.any { it == TaskType.Convert }) {
        val extractEvent =  events.lastOrNull { it.isOfEvent(Events.EventMediaParameterExtractCreated) }
        if (extractEvent == null || extractEvent.isSuccess() && convertEvent == null) {
            response[TaskType.Convert] = true
            log.info { "Waiting for ${Events.EventMediaParameterExtractCreated}" }
        }
    }

    if (tasks.contains(TaskType.Extract)) {
        if (events.lastOrNull { it.isOfEvent(
                Events.EventMediaParameterExtractCreated
            ) } == null) {
            response[TaskType.Extract] = true
            log.info { "Waiting for ${Events.EventMediaParameterExtractCreated}" }

        }
    }


    return response
}


fun isAwaitingTask(task: TaskType, events: List<Event>): Boolean {
    val taskStatus = when (task) {
        TaskType.Encode -> {
            val argumentEvent = Events.EventMediaParameterEncodeCreated
            val taskCreatedEvent = Events.EventWorkEncodeCreated
            val taskCompletedEvent = Events.EventWorkEncodePerformed

            val argument = events.findLast { it.event == argumentEvent } ?: return true
            if (!argument.isSuccess()) return false

            val trailingEvents = PersistentMessageHelper(events).getEventsRelatedTo(argument.eventId).filter {
                it.event in listOf(
                    argumentEvent,
                    taskCreatedEvent,
                    taskCompletedEvent
                )
            }

            val waiting = trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size != trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size
            waiting
        }
        TaskType.Extract -> {
            val argumentEvent = Events.EventMediaParameterExtractCreated
            val taskCreatedEvent = Events.EventWorkExtractCreated
            val taskCompletedEvent = Events.EventWorkExtractPerformed

            val argument = events.findLast { it.event == argumentEvent } ?: return true
            if (!argument.isSuccess()) return false
            val trailingEvents = PersistentMessageHelper(events).getEventsRelatedTo(argument.eventId).filter {
                it.event in listOf(
                    argumentEvent,
                    taskCreatedEvent,
                    taskCompletedEvent
                )
            }
            val waiting = trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size != trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size
            waiting
        }
        TaskType.Convert -> {

            val extractEvents = events.findLast { it.isOfEvent(Events.EventMediaParameterExtractCreated) }
            if (extractEvents == null || extractEvents.isSkipped()) {
                false
            } else {
                val taskCreatedEvent = Events.EventWorkConvertCreated
                val taskCompletedEvent = Events.EventWorkConvertPerformed

                val argument = events.findLast { it.event == taskCreatedEvent } ?: return true
                if (!argument.isSuccess()) return false

                val trailingEvents = PersistentMessageHelper(events).getEventsRelatedTo(argument.eventId).filter {
                    it.event in listOf(
                        taskCreatedEvent,
                        taskCompletedEvent
                    )
                }
                val waiting = trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size != trailingEvents.filter { it.isOfEvent(taskCreatedEvent) }.size
                waiting
            }
        }
    }
    if (taskStatus) {
        log.info { "isAwaiting for $task" }
    }
    return taskStatus
}*/