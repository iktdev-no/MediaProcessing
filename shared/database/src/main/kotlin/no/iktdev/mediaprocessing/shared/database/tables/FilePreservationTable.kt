package no.iktdev.mediaprocessing.shared.database.tables

import no.iktdev.mediaprocessing.shared.common.UtcNow
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object FilePreservationTable : LongIdTable("FILE_PRESERVATION") {
    val fileName = varchar("FILE_NAME", 500)
    val filePath = text("FILE_PATH") // Støtter megastore stier uten kutt
    val preserved = bool("PRESERVED").default(false)
    val persistedAt = timestamp("PERSISTED_AT")
        .clientDefault { UtcNow() }
}