package no.iktdev.mediaprocessing.shared.common.contract

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.isSuccessful

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