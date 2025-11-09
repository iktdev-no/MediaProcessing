package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.eventi.models.Event


inline fun <reified T: Event> Event.az(): T? {
    return if (this !is T) {
        System.err.println("${this::class.java.name} is not a type of ${T::class.java.name}")
        null
    } else this
}