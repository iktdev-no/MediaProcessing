package no.iktdev.mediaprocessing.shared.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*

object EventsTable: IntIdTable(name = "EVENTS") {
    val referenceId: Column<UUID> = uuid("REFERENCE_ID")
    val eventId: Column<UUID> = uuid("EVENT_ID")
    val event: Column<String> = varchar("EVENT",100)
    val data: Column<String> = text("DATA")
    val persistedAt: Column<LocalDateTime> = datetime("PERSISTED_AT").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(referenceId, eventId, event)
    }
}