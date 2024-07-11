package no.iktdev.eventi.mock.data

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.EventMetadata

data class SecondEvent(
    override val metadata: EventMetadata,
    override val eventType: String = "Second",
    override val data: ElementsToCreate = ElementsToCreate()
): EventImpl() {
}

data class ElementsToCreate(
    val elements: List<String> = listOf("eple", "banan", "appelsin", "drue", "pære")
)