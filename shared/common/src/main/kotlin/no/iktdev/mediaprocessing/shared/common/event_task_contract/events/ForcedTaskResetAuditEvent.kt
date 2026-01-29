package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import java.util.*

data class ForcedTaskResetAuditEvent(
    val taskId: UUID,
): Event()