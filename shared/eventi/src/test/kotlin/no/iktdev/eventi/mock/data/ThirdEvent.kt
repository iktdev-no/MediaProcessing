package no.iktdev.eventi.mock.data

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.EventMetadata

data class ThirdEvent(
    override val metadata: EventMetadata,
    override val eventType: String = "Third",
    override val data: String
): EventImpl() {
}