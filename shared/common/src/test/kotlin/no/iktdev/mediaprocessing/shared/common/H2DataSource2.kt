package no.iktdev.mediaprocessing.shared.common

import no.iktdev.mediaprocessing.shared.common.datasource.DatabaseConnectionConfig
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import org.jetbrains.exposed.sql.Database

class H2DataSource2(conf: DatabaseConnectionConfig): MySqlDataSource(conf) {

    override fun createDatabaseStatement(): String {
        return "CREATE SCHEMA ${config.databaseName};"
    }

    override fun toDatabaseConnectionUrl(database: String): String {
        return toConnectionUrl()
    }
    override fun toDatabase(): Database {
        return super.toDatabase()
    }
    override fun toConnectionUrl(): String {
        return "jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;"
    }

}