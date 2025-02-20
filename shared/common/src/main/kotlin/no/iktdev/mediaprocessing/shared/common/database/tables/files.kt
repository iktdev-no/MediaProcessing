package no.iktdev.mediaprocessing.shared.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object files: IntIdTable() {
    val baseName: Column<String> = varchar("baseName", 256)
    val folder: Column<String> = varchar("folder", 256)
    val fileName: Column<String> = varchar("fileName", 512)
    val checksum: Column<String> = varchar("checksum", 256).uniqueIndex()
}