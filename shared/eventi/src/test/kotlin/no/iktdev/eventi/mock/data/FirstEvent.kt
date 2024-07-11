package no.iktdev.eventi.mock.data

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.EventMetadata

data class FirstEvent(
    override val metadata: EventMetadata,
    override val eventType: String = "First",
    override val data: String
): EventImpl() {
}