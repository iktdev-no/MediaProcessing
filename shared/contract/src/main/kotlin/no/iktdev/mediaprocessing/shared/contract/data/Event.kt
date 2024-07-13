package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

abstract class Event: EventImpl() {
    abstract override val eventType: Events
}

inline fun <reified T: Event> Event.az(): T? {
    return if (this !is T) {
        System.err.println("${this::class.java.name} is not a type of ${T::class.java.name}")
        null
    } else this
}
