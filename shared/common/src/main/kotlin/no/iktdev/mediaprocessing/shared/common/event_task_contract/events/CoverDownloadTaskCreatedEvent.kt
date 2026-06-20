package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.MultiTaskCreatedEvent
import no.iktdev.eventi.models.MultiTaskIdentity
import java.util.UUID

class CoverDownloadTaskCreatedEvent(taskIds: Set<MultiTaskIdentity>): MultiTaskCreatedEvent(taskIds) {
}