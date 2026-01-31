package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.SignalEvent
import java.util.*

data class ForcedTaskResetAuditEvent(
    val taskId: UUID,
): SignalEvent()