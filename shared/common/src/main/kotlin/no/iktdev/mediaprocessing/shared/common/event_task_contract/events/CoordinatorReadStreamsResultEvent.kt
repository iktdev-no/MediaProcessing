package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import com.google.gson.JsonObject
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus

data class CoordinatorReadStreamsResultEvent(
    val data: JsonObject? = null,
    val status: TaskStatus
): Event() {
}