package no.iktdev.mediaprocessing.shared.common.database.tables

import no.iktdev.mediaprocessing.shared.common.database.tables.runners.defaultExpression
import no.iktdev.mediaprocessing.shared.common.database.tables.tasks.default
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object processed: IntIdTable() {
    val title: Column<String> = varchar("title", 256)
    val fileName: Column<String> = varchar("fileName", 256)
    val processedFiles: Column<String> = text("processedFilesJson")
    val encoded: Column<Boolean> = bool("encoded").default(false)
    val extracted: Column<Boolean> = bool("extracted").default(false)
    val created: Column<LocalDateTime> = datetime("created").defaultExpression(CurrentDateTime)
}