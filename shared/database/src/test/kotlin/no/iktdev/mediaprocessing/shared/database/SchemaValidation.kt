package no.iktdev.mediaprocessing.shared.database

import no.iktdev.mediaprocessing.shared.database.tables.EventsTable
import no.iktdev.mediaprocessing.shared.database.tables.FilesTable
import no.iktdev.mediaprocessing.shared.database.tables.TasksTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.junit.jupiter.api.Test

class SchemaValidation: TestBaseWithDatabase() {

    @Test
    fun verifySchema() {
        withTransaction {
            val allTables = listOf(
                EventsTable,
                TasksTable,
                FilesTable
            )
            SchemaUtils.checkMappingConsistence(*allTables.toTypedArray())
        }
    }


}