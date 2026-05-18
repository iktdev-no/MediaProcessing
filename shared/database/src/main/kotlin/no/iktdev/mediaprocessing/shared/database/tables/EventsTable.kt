package no.iktdev.mediaprocessing.shared.database.tables

import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.mediaprocessing.shared.common.UtcNow
import no.iktdev.mediaprocessing.shared.database.LongTextColumnType
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import java.util.*

object EventsTable: IntIdTable(name = "EVENTS") {
    val referenceId: Column<String> = varchar("REFERENCE_ID", 36)
    val eventId: Column<String> = varchar("EVENT_ID", 36)
    val event: Column<String> = varchar("EVENT",100)
    val data = registerColumn<String>("DATA", LongTextColumnType())
    val persistedAt = timestamp("PERSISTED_AT")
        .clientDefault { UtcNow() }


    init {
        uniqueIndex(referenceId, eventId, event)
    }

    fun getWhere(predicate: SqlExpressionBuilder.() -> Op<Boolean>): List<PersistedEvent> {
        return EventsTable.selectAll()
            .where(predicate)
            .orderBy(EventsTable.id, SortOrder.DESC)
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

    fun getFromQuery(query: Query): List<PersistedEvent> {
        return query
            .orderBy(EventsTable.id, SortOrder.DESC)
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



}