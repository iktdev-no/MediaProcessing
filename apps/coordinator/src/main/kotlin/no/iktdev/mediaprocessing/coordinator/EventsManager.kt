package no.iktdev.mediaprocessing.coordinator

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.implementations.EventsManagerImpl
import no.iktdev.mediaprocessing.shared.common.datasource.DataSource
import no.iktdev.mediaprocessing.shared.contract.EventsManagerContract
import no.iktdev.mediaprocessing.shared.contract.data.Event

class EventsManager(dataSource: DataSource) : EventsManagerContract(dataSource) {
    override fun readAvailableEvents(): List<Event> {
        TODO("Not yet implemented")
    }

    override fun readAvailableEventsFor(referenceId: String): List<Event> {
        TODO("Not yet implemented")
    }

    override fun storeEvent(event: Event): Boolean {
        TODO("Not yet implemented")
    }
}

class MockEventManager(dataSource: DataSource) : EventsManagerImpl<EventImpl>(dataSource) {
    val events: MutableList<EventImpl> = mutableListOf()
    override fun readAvailableEvents(): List<EventImpl> {
        return events.toList()
    }

    override fun readAvailableEventsFor(referenceId: String): List<EventImpl> {
        return events.filter { it.metadata.referenceId == referenceId }
    }

    override fun storeEvent(event: EventImpl): Boolean {
        return events.add(event)
    }
}