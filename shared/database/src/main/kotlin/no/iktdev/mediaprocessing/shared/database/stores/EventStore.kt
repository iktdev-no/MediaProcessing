package no.iktdev.mediaprocessing.shared.database.stores

import no.iktdev.eventi.ZDS
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.stores.EventStore
import no.iktdev.mediaprocessing.shared.common.UtcNow
import no.iktdev.mediaprocessing.shared.database.tables.EventsTable
import no.iktdev.mediaprocessing.shared.database.withTransaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime
import java.util.*

object EventStore: EventStore {
    override fun getPersistedEventsAfter(timestamp: LocalDateTime): List<PersistedEvent> {
        val result = withTransaction {
            EventsTable.selectAll()
                .where { EventsTable.persistedAt greaterEq timestamp }
                .map {
                    PersistedEvent(
                        id = it[EventsTable.id].value.toLong(),
                        referenceId = UUID.fromString(it[EventsTable.referenceId]),
                        eventId = UUID.fromString(it[EventsTable.eventId]),
                        event = it[EventsTable.event],
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
                .where { EventsTable.referenceId eq referenceId.toString()}
                .map {
                    PersistedEvent(
                        id = it[EventsTable.id].value.toLong(),
                        referenceId = UUID.fromString(it[EventsTable.referenceId]),
                        eventId = UUID.fromString(it[EventsTable.eventId]),
                        event = it[EventsTable.event],
                        data = it[EventsTable.data],
                        persistedAt = it[EventsTable.persistedAt]
                    )
                }
        }
        return result.getOrDefault(emptyList())
    }

    override fun persist(event: Event) {
        val asData = ZDS.WGson.toJson(event)
        val eventName = event::class.simpleName ?: run {
            throw RuntimeException("Missing class name for event: $event")
        }
        withTransaction {
            EventsTable.insert {
                it[EventsTable.referenceId] = event.referenceId.toString()
                it[EventsTable.eventId] = event.eventId.toString()
                it[EventsTable.event] = eventName
                it[EventsTable.data] = asData
                it[EventsTable.persistedAt] = UtcNow()
            }
        }
    }
}