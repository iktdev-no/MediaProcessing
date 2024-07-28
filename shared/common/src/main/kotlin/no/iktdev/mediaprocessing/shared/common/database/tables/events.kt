package no.iktdev.mediaprocessing.shared.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object events: IntIdTable() {
    val referenceId: Column<String> = varchar("referenceId", 50)
    val eventId: Column<String> = varchar("eventId", 50)
    val event: Column<String> = varchar("event",100)
    val data: Column<String> = text("data")
    //val success: Column<Boolean> = bool("success").default(false)
    val created: Column<LocalDateTime> = datetime("created").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(referenceId, eventId, event)
    }
}