package no.iktdev.eventi.implementations

import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.data.*

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

    open fun isPrerequisitesFulfilled(incomingEvent: T, events: List<T>): Boolean {
        return true
    }

    open fun shouldIHandleFailedEvents(incomingEvent: T): Boolean {
        return false
    }

    open fun haveProducedExpectedMessageBasedOnEvent(incomingEvent: T, events: List<T>): Boolean {
        val eventsProducedByListener = events.filter { it.eventType == produceEvent }
        val triggeredBy = events.filter { it.eventType in listensForEvents }
        return eventsProducedByListener.any { it.derivedFromEventId() in triggeredBy.map { t -> t.eventId() } }
    }

    open fun shouldIProcessAndHandleEvent(incomingEvent: T, events: List<T>): Boolean {
        if (!isOfEventsIListenFor(incomingEvent))
            return false
        if (!isPrerequisitesFulfilled(incomingEvent, events)) {
            return false
        }
        if (!incomingEvent.isSuccessful() && !shouldIHandleFailedEvents(incomingEvent)) {
            return false
        }

        val childOf = events.filter { it.derivedFromEventId() == incomingEvent.eventId() }
        val haveListenerProduced = childOf.any { it.eventType == produceEvent }
        if (haveListenerProduced)
            return false

        if (haveProducedExpectedMessageBasedOnEvent(incomingEvent, events))
            return false

        //val isDerived = events.any { it.metadata.derivedFromEventId == incomingEvent.metadata.eventId } // && incomingEvent.eventType == produceEvent
        return true
    }

    /**
     * @param incomingEvent Can be a new event or iterated form sequence in order to re-produce events
     * @param events Will be all available events for collection with the same reference id
     * @return boolean if read or not
     */
    abstract fun onEventsReceived(incomingEvent: ConsumableEvent<T>, events: List<T>)

    fun T.makeDerivedEventInfo(status: EventStatus): EventMetadata {
        return EventMetadata(
            referenceId = this.metadata.referenceId,
            derivedFromEventId = this.metadata.eventId,
            status = status
        )
    }


}