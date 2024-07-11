package no.iktdev.eventi.mock

import no.iktdev.eventi.database.DatabaseConnectionConfig
import no.iktdev.mediaprocessing.shared.common.datasource.DataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table


val fakeDatabasConfig = DatabaseConnectionConfig(
    address = "0.0.0.0",
    port = "3033",
    username = "TST",
    password = "TST",
    databaseName = "events"
)

class MockDataSource(): DataSource(fakeDatabasConfig) {
    override fun connect() {}

    override fun createDatabase(): Database? { return null }

    override fun createTables(vararg tables: Table) {}

    override fun createDatabaseStatement(): String { return "" }

    override fun toConnectionUrl(): String { return "" }

    override fun toDatabaseConnectionUrl(database: String): String { return "" }

    override fun toDatabase(): Database { TODO("Not yet implemented") }

}