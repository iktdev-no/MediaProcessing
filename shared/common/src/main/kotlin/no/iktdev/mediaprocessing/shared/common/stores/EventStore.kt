package no.iktdev.mediaprocessing.shared.common.stores

import com.google.gson.Gson
import no.iktdev.eventi.ZDS.toPersisted
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.stores.EventStore
import no.iktdev.mediaprocessing.shared.common.database.tables.EventsTable
import no.iktdev.mediaprocessing.shared.common.database.withTransaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime
import java.util.UUID

object EventStore: EventStore {
    override fun getPersistedEventsAfter(timestamp: LocalDateTime): List<PersistedEvent> {
        val result = withTransaction {
            EventsTable.selectAll()
                .where { EventsTable.persistedAt greater timestamp }
                .map {
                    PersistedEvent(
                        id = it[EventsTable.id].value.toLong(),
                        referenceId = it[EventsTable.referenceId],
                        eventId = it[EventsTable.eventId],
                        event = "", // You might want to store the event type as well
                        data = it[EventsTable.data],
                        persistedAt = it[EventsTable.persistedAt]
                    )
                }
        }
        return result.getOrDefault(emptyList())
    }

    override fun getPersistedEventsFor(referenceId: UUID): List<PersistedEvent> {
        val result = withTransaction {
            EventsTable.selectAll()
                .where { EventsTable.referenceId eq referenceId}
                .map {
                    PersistedEvent(
                        id = it[EventsTable.id].value.toLong(),
                        referenceId = it[EventsTable.referenceId],
                        eventId = it[EventsTable.eventId],
                        event = "", // You might want to store the event type as well
                        data = it[EventsTable.data],
                        persistedAt = it[EventsTable.persistedAt]
                    )
                }
        }
        return result.getOrDefault(emptyList())
    }

    override fun persist(event: Event) {
        val asData = Gson().toJson(event)
        val eventName = event::class.simpleName ?: run {
            throw RuntimeException("Missing class name for event: $event")
        }
        withTransaction {
            EventsTable.insert {
                it[EventsTable.referenceId] = event.referenceId
                it[EventsTable.eventId] = event.eventId
                it[EventsTable.event] = eventName
                it[EventsTable.data] = asData
                it[EventsTable.persistedAt] = LocalDateTime.now()
            }
        }
    }
}