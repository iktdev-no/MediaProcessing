package no.iktdev.mediaprocessing.shared.common.persistance

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object processerEvents: IntIdTable() {
    val referenceId: Column<String> = varchar("referenceId", 50)
    val status: Column<String?> = varchar("status", 10).nullable()
    val claimed: Column<Boolean> = bool("claimed").default(false)
    val claimedBy: Column<String?> = varchar("claimedBy", 100).nullable()
    val event: Column<String> = varchar("event",100)
    val eventId: Column<String> = varchar("eventId", 50)
    val data: Column<String> = text("data")
    val consumed: Column<Boolean> = bool("consumed").default(false)
    val created: Column<LocalDateTime> = datetime("created").defaultExpression(CurrentDateTime)
    val lastCheckIn: Column<LocalDateTime?> = datetime("lastCheckIn").nullable()

    init {
        uniqueIndex(referenceId, eventId, event)
    }
}