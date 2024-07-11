package no.iktdev.eventi.implementations

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.data.isSuccessful

abstract class EventListenerImpl<T: EventImpl, E: EventsManagerImpl<T>> {
    abstract val coordinator: EventCoordinator<T, E>?

    abstract val produceEvent: Any
    abstract val listensForEvents: List<Any>

    protected open fun onProduceEvent(event: T) {
        coordinator?.produceNewEvent(event) ?: {
            println("No Coordinator set")
        }
    }

    open fun isOfEventsIListenFor(event: T): Boolean {
        return listensForEvents.any { it == event.eventType }
    }

    open fun <T: EventImpl> isPrerequisitesFulfilled(incomingEvent: T, events: List<T>): Boolean {
        return true
    }

    open fun shouldIProcessAndHandleEvent(incomingEvent: T, events: List<T>): Boolean {
        if (!isOfEventsIListenFor(incomingEvent))
            return false
        if (!isPrerequisitesFulfilled(incomingEvent, events)) {
            return false
        }
        if (!incomingEvent.isSuccessful()) {
            return false
        }
        val isDerived = events.any { it.metadata.derivedFromEventId == incomingEvent.metadata.eventId } // && incomingEvent.eventType == produceEvent
        return !isDerived
    }

    /**
     * @param incomingEvent Can be a new event or iterated form sequence in order to re-produce events
     * @param events Will be all available events for collection with the same reference id
     * @return boolean if read or not
     */
    abstract fun onEventsReceived(incomingEvent: T, events: List<T>)

    fun T.makeDerivedEventInfo(status: EventStatus): EventMetadata {
        return EventMetadata(
            referenceId = this.metadata.referenceId,
            derivedFromEventId = this.metadata.eventId,
            status = status
        )
    }


}