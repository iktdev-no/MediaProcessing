package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import java.util.UUID

class FilePrepareForWorkResultEvent(
    status: TaskStatus,
    val file: String? = null,
    error: String? = null,
): TaskResultEvent(status = status, error = error) {}