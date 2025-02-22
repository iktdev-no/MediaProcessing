package no.iktdev.mediaprocessing.shared.common.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.common.contract.Events

data class PermitWorkCreationEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.WorkProceedPermitted,
    override val data: String?
) : Event() {
}