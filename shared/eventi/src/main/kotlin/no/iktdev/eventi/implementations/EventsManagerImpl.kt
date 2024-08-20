package no.iktdev.eventi.implementations

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.database.DataSource

/**
 * Interacts with the database, needs to be within the Coordinator
 */
abstract class EventsManagerImpl<T: EventImpl>(val dataSource: DataSource) {

    abstract fun getAvailableReferenceIds(): List<String>
    abstract fun readAvailableEvents(): List<List<T>>
    abstract fun readAvailableEventsFor(referenceId: String): List<T>

    abstract fun getAllEvents(): List<List<T>>
    abstract fun getEventsWith(referenceId: String): List<T>

    abstract fun storeEvent(event: T): Boolean
}