package no.iktdev.mediaprocessing.shared.database

import no.iktdev.eventi.MyTime
import no.iktdev.eventi.ZDS.toPersisted
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.stores.EventStore
import java.time.Instant
import java.util.*

class InMemoryEventStore : EventStore {
    private val persisted = mutableListOf<PersistedEvent>()
    private var nextId = 1L

    override fun getPersistedEventsAfter(timestamp: Instant): List<PersistedEvent> =
        persisted.filter { it.persistedAt > timestamp }

    override fun getPersistedEventsFor(referenceId: UUID): List<PersistedEvent> =
        persisted.filter { it.referenceId == referenceId }

    override fun persist(event: Event) {
        val persistedEvent = event.toPersisted(nextId++, MyTime.utcNow())
        persisted += persistedEvent!!
    }

    fun persistAt(event: Event, persistedAt: Instant) {
        val persistedEvent = event.toPersisted(nextId++, persistedAt)
        persisted += persistedEvent!!
    }

    fun setHistory(events: List<Event>) {
        events.forEach { persist(it) }
    }

    fun all(): List<PersistedEvent> = persisted
    fun clear() { persisted.clear(); nextId = 1L }
}
