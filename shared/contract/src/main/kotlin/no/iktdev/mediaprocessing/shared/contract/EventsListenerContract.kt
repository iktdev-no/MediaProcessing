package no.iktdev.mediaprocessing.shared.contract

import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.eventi.implementations.EventListenerImpl
import no.iktdev.mediaprocessing.shared.contract.data.Event

abstract class EventsListenerContract<E: EventsManagerContract, C: EventCoordinator<Event, E>>: EventListenerImpl<Event, E>() {
    abstract override val produceEvent: Events
    abstract override val listensForEvents: List<Events>
    abstract override val coordinator: C?
}
