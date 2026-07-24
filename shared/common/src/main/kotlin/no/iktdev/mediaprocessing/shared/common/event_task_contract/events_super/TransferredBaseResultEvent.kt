package no.iktdev.mediaprocessing.shared.common.event_task_contract.events_super

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.CoverTransferredResultEvent

abstract class TransferredBaseResultEvent(
    status: TaskStatus,
    error: String? = null
) : TaskResultEvent(status, error) {
}