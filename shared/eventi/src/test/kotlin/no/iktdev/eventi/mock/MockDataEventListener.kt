package no.iktdev.eventi.mock

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.eventi.implementations.EventListenerImpl

abstract class MockDataEventListener() : EventListenerImpl<EventImpl, MockEventManager>() {
    abstract override val produceEvent: Any
    abstract override val listensForEvents: List<Any>
    abstract override val coordinator: MockEventCoordinator?
}