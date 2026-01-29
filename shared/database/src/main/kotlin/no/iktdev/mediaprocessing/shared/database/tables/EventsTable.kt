package no.iktdev.mediaprocessing.shared.database.tables

import no.iktdev.mediaprocessing.shared.common.UtcNow
import no.iktdev.mediaprocessing.shared.database.LongTextColumnType
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp

object EventsTable: IntIdTable(name = "EVENTS") {
    val referenceId: Column<String> = varchar("REFERENCE_ID", 36)
    val eventId: Column<String> = varchar("EVENT_ID", 36)
    val event: Column<String> = varchar("EVENT",100)
    val data = registerColumn<String>("data", LongTextColumnType())
    val persistedAt = timestamp("PERSISTED_AT")
        .clientDefault { UtcNow() }


    init {
        uniqueIndex(referenceId, eventId, event)
    }
}