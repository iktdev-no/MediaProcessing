package no.iktdev.mediaprocessing.shared.database.stores

import no.iktdev.eventi.ZDS
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.stores.EventStore
import no.iktdev.mediaprocessing.shared.common.UtcNow
import no.iktdev.mediaprocessing.shared.common.dto.EventQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.database.queries.pagedQuery
import no.iktdev.mediaprocessing.shared.database.tables.EventsTable
import no.iktdev.mediaprocessing.shared.database.withTransaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.util.*

object EventStore: EventStore {

    fun getPagedEvents(query: EventQuery): Paginated<PersistedEvent> =
        pagedQuery(
            table = EventsTable,
            query = query,
            sortColumns = mapOf(
                "referenceId" to EventsTable.referenceId,
                "eventId" to EventsTable.eventId,
                "event" to EventsTable.event,
                "persistedAt" to EventsTable.persistedAt
            ),
            applyFilters = {

                query.referenceId?.let { ref ->
                    where { EventsTable.referenceId like "%$ref%" }
                }

                query.eventId?.let { id ->
                    where { EventsTable.eventId like "%$id%" }
                }

                query.key?.let { ev ->
                    where { EventsTable.event like "%$ev%" }
                }

                query.from?.let { from ->
                    where { EventsTable.persistedAt greaterEq from }
                }

                query.to?.let { to ->
                    where { EventsTable.persistedAt lessEq to }
                }
            },
            mapper = { row ->
                PersistedEvent(
                    id = row[EventsTable.id].value.toLong(),
                    referenceId = UUID.fromString(row[EventsTable.referenceId]),
                    eventId = UUID.fromString(row[EventsTable.eventId]),
                    event = row[EventsTable.event],
                    data = row[EventsTable.data],
                    persistedAt = row[EventsTable.persistedAt]
                )
            }
        )


    override fun getPersistedEventsAfter(timestamp: Instant): List<PersistedEvent> {
        val result = withTransaction {
            EventsTable.selectAll()
                .where { EventsTable.persistedAt greater timestamp }
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