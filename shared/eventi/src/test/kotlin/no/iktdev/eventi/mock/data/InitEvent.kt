package no.iktdev.eventi.mock.data

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.EventMetadata

data class InitEvent(
    override val metadata: EventMetadata,
    override val eventType: String = "Init",
    override val data: String
): EventImpl() {
}