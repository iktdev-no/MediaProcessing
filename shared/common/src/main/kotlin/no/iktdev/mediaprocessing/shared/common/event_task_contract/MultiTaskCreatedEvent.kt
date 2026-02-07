package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import java.util.UUID

/**
 * Base class, should not be serialized into
 */
open class MultiTaskCreatedEvent(
    val taskIds: List<UUID>,
) : Event()
