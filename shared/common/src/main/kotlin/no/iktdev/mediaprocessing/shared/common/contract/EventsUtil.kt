package no.iktdev.mediaprocessing.shared.common.contract

import mu.KotlinLogging
import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.isSuccessful
import no.iktdev.mediaprocessing.shared.common.contract.data.Event

private val log = KotlinLogging.logger {}


fun List<EventImpl>.lastOrSuccess(): EventImpl? {
    return this.lastOrNull { it.isSuccessful() } ?: this.lastOrNull()
}

fun List<EventImpl>.lastOrSuccessOf(event: no.iktdev.mediaprocessing.shared.common.contract.Events): EventImpl? {
    val validEvents = this.filter { it.eventType == event }
    return validEvents.lastOrNull { it.isSuccessful() } ?: validEvents.lastOrNull()
}

fun List<EventImpl>.lastOrSuccessOf(event: no.iktdev.mediaprocessing.shared.common.contract.Events, predicate: (EventImpl) -> Boolean): EventImpl? {
    val validEvents = this.filter { it.eventType == event && predicate(it) }
    return validEvents.lastOrNull()
}

