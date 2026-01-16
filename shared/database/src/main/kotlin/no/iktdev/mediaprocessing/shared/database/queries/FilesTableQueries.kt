package no.iktdev.mediaprocessing.shared.database.queries

import no.iktdev.mediaprocessing.shared.common.dto.FileTableItem
import no.iktdev.mediaprocessing.shared.database.tables.FilesTable
import no.iktdev.mediaprocessing.shared.database.withTransaction
import org.jetbrains.exposed.sql.selectAll

class FilesTableQueries {
    fun getFiles(): List<FileTableItem> {
        return withTransaction {
            FilesTable.selectAll()
                .mapNotNull {
                    FileTableItem(
                        name = it[FilesTable.name],
                        uri = it[FilesTable.uri],
                        checksum = it[FilesTable.checksum],
                        identifiedAt = it[FilesTable.identifiedAt],
                    )
                }
        }.getOrElse { emptyList() }
    }
}