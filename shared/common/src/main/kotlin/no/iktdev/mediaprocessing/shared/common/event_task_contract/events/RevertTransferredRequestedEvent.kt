package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import java.util.UUID

class RevertTransferredRequestedEvent(
    val taskIds: Set<UUID>,
): Event() {
}