package no.iktdev.eventi.core

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.EventMetadata

class ConsumableEvent<T: EventImpl>(private var event: T) {
    var isConsumed: Boolean = false
        private set
    fun consume(): T? {
        return if (!isConsumed) {
            isConsumed = true
            event
        } else null
    }

    fun metadata(): EventMetadata {
        return event.metadata
    }
}