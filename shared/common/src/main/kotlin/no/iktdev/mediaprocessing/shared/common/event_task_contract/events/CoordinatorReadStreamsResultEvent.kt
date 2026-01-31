package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import com.google.gson.JsonObject
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

class CoordinatorReadStreamsResultEvent(
    val data: JsonObject? = null,
    status: TaskStatus,
    error: String? = null
) : TaskResultEvent(status, error)
