package no.iktdev.mediaprocessing.shared.common.database.queries

import no.iktdev.mediaprocessing.shared.common.database.tables.FilesTable
import no.iktdev.mediaprocessing.shared.common.database.withTransaction
import no.iktdev.mediaprocessing.shared.common.dto.FileTableItem
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