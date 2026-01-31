package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus

/**
 * Base class, should not be serialized into
 */
open class TaskResultEvent(
    val status: TaskStatus,
    val error: String? = null
) : Event()
