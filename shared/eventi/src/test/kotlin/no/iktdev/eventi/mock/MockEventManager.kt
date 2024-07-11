package no.iktdev.eventi.mock

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.implementations.EventsManagerImpl
import org.springframework.stereotype.Component

@Component
class MockEventManager(dataSource: MockDataSource = MockDataSource()) : EventsManagerImpl<EventImpl>(dataSource) {
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
