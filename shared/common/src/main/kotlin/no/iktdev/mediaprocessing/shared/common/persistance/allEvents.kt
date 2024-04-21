package no.iktdev.mediaprocessing.shared.common.persistance

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object allEvents: IntIdTable() {
    val referenceId: Column<String> = varchar("referenceId", 50)
    val eventId: Column<String> = varchar("eventId", 50)
    val event: Column<String> = varchar("event",100)
    val data: Column<String> = text("data")
    val integrity: Column<String> = varchar("integrity", 100)
    //val success: Column<Boolean> = bool("success").default(false)
    val created: Column<LocalDateTime> = datetime("created").defaultExpression(CurrentDateTime)
}