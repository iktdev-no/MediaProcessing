package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Metadata
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.CoverTransferredResultEvent
import java.util.UUID

/**
 * Base class, should not be serialized into
 */
abstract class TaskResultEvent(
    val status: TaskStatus,
    val error: String? = null,
    val logFile: String? = null
) : Event() {

    abstract fun newStatus(ns: TaskStatus): TaskResultEvent

    protected fun from(original: TaskResultEvent): TaskResultEvent {
        return this.apply {
            this.metadata = Metadata().derivedFromEventId(original.metadata.derivedFromId ?: emptySet())
            this.usingReferenceId(original.referenceId)
        }
    }


}
