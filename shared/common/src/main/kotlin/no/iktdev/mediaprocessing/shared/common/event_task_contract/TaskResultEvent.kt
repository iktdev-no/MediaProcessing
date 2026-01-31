package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus

/**
 * Base class, should not be serialized into
 */
abstract class TaskResultEvent(
    open val status: TaskStatus,
    open val error: String? = null
) : Event()
