package no.iktdev.eventi.implementations

import no.iktdev.eventi.data.EventImpl
import no.iktdev.mediaprocessing.shared.common.datasource.DataSource

/**
 * Interacts with the database, needs to be within the Coordinator
 */
abstract class EventsManagerImpl<T: EventImpl>(val dataSource: DataSource) {
    abstract fun readAvailableEvents(): List<T>

    abstract fun readAvailableEventsFor(referenceId: String): List<T>

    abstract fun storeEvent(event: T): Boolean
}