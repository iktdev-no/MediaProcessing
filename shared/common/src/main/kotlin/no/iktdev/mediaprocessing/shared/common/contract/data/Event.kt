package no.iktdev.mediaprocessing.shared.common.contract.data

import no.iktdev.eventi.data.EventImpl
import no.iktdev.mediaprocessing.shared.common.contract.Events

abstract class Event: EventImpl() {
    abstract override val eventType: Events
}

inline fun <reified T: Event> Event.az(): T? {
    return if (this !is T) {
        System.err.println("${this::class.java.name} is not a type of ${T::class.java.name}")
        null
    } else this
}

inline fun <reified T: Event> List<Event>.findFirstEventOf(): T? {
    val first = this.firstOrNull { it is T }
    return if (first != null) {
        first as T
    } else null
}

inline fun List<Event>.findFirstOf(events: Events): Event? {
    return this.firstOrNull { it.eventType == events }
}

inline fun <reified T: Event> List<Event>.findEventsOf(): List<T> {
    return this.filterIsInstance<T>().map { it }
}