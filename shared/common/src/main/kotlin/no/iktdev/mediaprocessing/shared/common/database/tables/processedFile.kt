package no.iktdev.mediaprocessing.shared.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object processedFile: IntIdTable() {
    val title: Column<String> = varchar("title", 256)
    val inputFile: Column<String> = varchar("fileName", 512)
    val data: Column<String> = text("data")
    val created: Column<LocalDateTime> = datetime("created").defaultExpression(CurrentDateTime)
    val checksum: Column<String?> = varchar("checksum", 256).nullable()
}