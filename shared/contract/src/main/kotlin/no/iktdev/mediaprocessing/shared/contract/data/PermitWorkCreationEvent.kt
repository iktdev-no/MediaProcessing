package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class PermitWorkCreationEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.EventMediaWorkProceedPermitted,
    override val data: String?
) : Event() {
}