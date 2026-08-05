package no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer

import no.iktdev.eventi.models.MultiTaskCreatedEvent
import no.iktdev.eventi.models.MultiTaskIdentity
import java.util.UUID

class RevertTransferContentTaskCreatedEvent(groupId: UUID, taskIds: Set<MultiTaskIdentity>) : TransferContentTaskCreatedEvent(groupId, taskIds) {
}