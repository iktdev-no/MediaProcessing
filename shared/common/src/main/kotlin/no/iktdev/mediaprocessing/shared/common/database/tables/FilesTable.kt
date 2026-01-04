package no.iktdev.mediaprocessing.shared.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime

object FilesTable: IntIdTable("FILES") {
    val name: Column<String> = varchar("NAME", 255)
    val uri: Column<String> = text("URI")
    val checksum: Column<String> = char("CHECKSUM", 64)
    val identifiedAt: Column<java.time.LocalDateTime> = datetime("IDENTIFIED_AT")
}