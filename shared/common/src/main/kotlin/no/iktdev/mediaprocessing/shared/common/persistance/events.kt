package no.iktdev.mediaprocessing.shared.common.persistance

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object events: IntIdTable() {
    val referenceId: Column<String> = varchar("referenceId", 50)
    val eventId: Column<String> = varchar("eventId", 50)
    val event: Column<String> = varchar("event1",100)
    val data: Column<String> = text("data")
    val created: Column<LocalDateTime> = datetime("created").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(referenceId, eventId, event)
    }
}