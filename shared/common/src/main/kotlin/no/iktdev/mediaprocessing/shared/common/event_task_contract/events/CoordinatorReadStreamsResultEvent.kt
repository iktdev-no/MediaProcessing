package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import com.google.gson.JsonObject
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

data class CoordinatorReadStreamsResultEvent(
    val data: JsonObject? = null,
    override val status: TaskStatus,
    override val error: String? = null
) : TaskResultEvent(status, error)
