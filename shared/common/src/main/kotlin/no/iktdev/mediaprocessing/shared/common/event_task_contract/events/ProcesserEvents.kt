package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus

// Placeholder event, so that the listener does not continue to create tasks
class ProcesserExtractTaskCreatedEvent: Event() {
}

// Placeholder event, so that the listener does not continue to create tasks
class ProcesserEncodeTaskCreatedEvent: Event() {
}

// Placeholder event, so that the listener does not continue to create tasks
class ProcesserReadTaskCreatedEvent: Event() {
}


data class ProcesserEncodePerformedEvent(
    val data: EncodeResult
): Event() {

}

data class EncodeResult(
    val status: TaskStatus,
    val cachedOutputFile: String? = null
)


data class ProcesserExtractedPerformedEvent(
    val data: ExtractResult
): Event()

data class ExtractResult(
    val status: TaskStatus,
    val cachedOutputFile: String? = null
)