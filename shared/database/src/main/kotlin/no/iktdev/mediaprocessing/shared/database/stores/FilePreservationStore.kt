package no.iktdev.mediaprocessing.shared.database.stores

import no.iktdev.mediaprocessing.shared.common.dto.files.PreservedFile
import no.iktdev.mediaprocessing.shared.common.dto.files.PreservedFileRow
import no.iktdev.mediaprocessing.shared.common.files.IFilePreservationStore
import no.iktdev.mediaprocessing.shared.database.tables.FilePreservationTable
import no.iktdev.mediaprocessing.shared.database.tables.FilePreservationTable.fileName
import no.iktdev.mediaprocessing.shared.database.tables.FilePreservationTable.preserved
import no.iktdev.mediaprocessing.shared.database.withTransaction
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.time.Instant

object FilePreservationStore : IFilePreservationStore {

    private fun ResultRow.toFilePreservation() = PreservedFileRow(
        id = this[FilePreservationTable.id].value,
        fileName = this[fileName],
        filePath = this[FilePreservationTable.filePath],
        preserved = this[preserved],
        persistedAt = this[FilePreservationTable.persistedAt]
    )

    override fun persist(fileName: String, filePath: String): Boolean {
        return withTransaction {
            FilePreservationTable.upsert(
                FilePreservationTable.fileName,
                // Her definerer du hva som skal skje ved en duplikatkollisjon (UPDATE-delen)
                onUpdate = {
                    it[FilePreservationTable.filePath] = filePath
                    it[preserved] = true
                }
            ) {
                // Dette kjøres både ved Insert og brukes som grunnlag
                it[FilePreservationTable.fileName] = fileName
                it[FilePreservationTable.filePath] = filePath
                it[preserved] = true
                it[persistedAt] = Instant.now() // Settes KUN ved første insert (eller hvis onUpdate tillater det)
            }
        }.isSuccess
    }

    override fun persist(fi: List<Pair<String, String>>) {
        if (fi.isEmpty()) return

        withTransaction {
            fi.forEach { (fileName, filePath) ->
                FilePreservationTable.upsert(
                    // MySQL Does not support edits here.
                    onUpdate = {
                        // Oppdater stien hvis filnavnet finnes fra før,
                        // men rør IKKE persistedAt!
                        it[FilePreservationTable.filePath] = filePath
                        it[preserved] = true
                    }
                ) {
                    it[FilePreservationTable.fileName] = fileName
                    it[FilePreservationTable.filePath] = filePath
                    it[FilePreservationTable.preserved] = true
                    it[FilePreservationTable.persistedAt] = Instant.now()
                }
            }
        }
    }


    override fun delete(id: Long) {
        withTransaction {
            FilePreservationTable.deleteWhere { FilePreservationTable.id eq id }
        }
    }

    override fun deleteByFileName(fileName: String) {
        withTransaction {
            FilePreservationTable.deleteWhere { FilePreservationTable.fileName eq fileName }
        }
    }

    override fun deleteByFilePath(filePath: String) {
        withTransaction {
            FilePreservationTable.deleteWhere { FilePreservationTable.filePath eq filePath }
        }
    }

    override fun setPreservedStatus(id: Long, preserved: Boolean) {
        withTransaction {
            FilePreservationTable.update({ FilePreservationTable.id eq id }) {
                it[FilePreservationTable.preserved] = preserved
            }
        }
    }

    override fun setPreservedStatus(fileName: String, preserved: Boolean): Boolean {
        return withTransaction {
            FilePreservationTable.update({ FilePreservationTable.fileName eq fileName }) {
                it[FilePreservationTable.preserved] = preserved
            }
        }.isSuccess
    }

    override fun getByPreserved(preserved: Boolean): List<PreservedFile> {
        return withTransaction {
            FilePreservationTable.selectAll()
                .where { FilePreservationTable.preserved eq preserved }
                .map { it.toFilePreservation().toPreservedFile() }
        }.getOrDefault(emptyList())
    }

    override fun getAllPreserved(): List<PreservedFile> {
        return withTransaction {
            FilePreservationTable.selectAll()
                .where { FilePreservationTable.preserved eq true }
                .map { it.toFilePreservation().toPreservedFile() }
        }.getOrDefault(emptyList())
    }

    override fun getAll(): List<PreservedFile> {
        return withTransaction {
            FilePreservationTable.selectAll()
                .map { it.toFilePreservation().toPreservedFile() }
        }.getOrDefault(emptyList())
    }

    override fun getById(id: Long): PreservedFile? {
        return withTransaction {
            FilePreservationTable.selectAll()
                .where { FilePreservationTable.id eq id }
                .map { it.toFilePreservation().toPreservedFile() }
                .singleOrNull()
        }.getOrNull()
    }

    override fun getByFileName(fileName: String): PreservedFile? {
        return withTransaction {
            FilePreservationTable.selectAll()
                .where { FilePreservationTable.fileName eq fileName }
                .map { it.toFilePreservation().toPreservedFile() }
                .singleOrNull()
        }.getOrNull()
    }
}