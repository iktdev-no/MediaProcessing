package no.iktdev.mediaprocessing.shared.common.persistance

import no.iktdev.mediaprocessing.shared.common.persistance.events.defaultExpression
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object runners: IntIdTable() {
    val startId: Column<String> = varchar("startId", 50)
    val application: Column<String> = varchar("application", 50)
    val version: Column<Int> = integer("version")
    val created: Column<LocalDateTime> = datetime("created").defaultExpression(CurrentDateTime)
}