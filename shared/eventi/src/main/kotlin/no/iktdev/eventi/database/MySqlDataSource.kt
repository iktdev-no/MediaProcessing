package no.iktdev.eventi.database

import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction


open class MySqlDataSource(conf: DatabaseConnectionConfig): DataSource(conf) {
    val log = KotlinLogging.logger {}
    override fun connect() {
        this.toDatabase()
    }

    override fun createDatabase(): Database? {
        val ok = transaction(toDatabaseServerConnection()) {
            val tmc = TransactionManager.current().connection
            val query = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '${config.databaseName}';"
            val stmt = tmc.prepareStatement(query, true)

            val resultSet = stmt.executeQuery()
            val databaseExists = resultSet.next()

            if (!databaseExists) {
                try {
                    exec(createDatabaseStatement())
                    log.info { "Database ${config.databaseName} created." }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            } else {
                log.info { "Database ${config.databaseName} already exists." }
                true
            }
        }

        return if (ok) toDatabase() else {
            log.error { "No database to create or connect to" }
            null
        }
    }

    override fun createTables(vararg tables: Table) {
        transaction(this.database) {
            SchemaUtils.createMissingTablesAndColumns(*tables)
            log.info { "Database transaction completed" }
        }
    }

    override fun createDatabaseStatement(): String {
        return "CREATE DATABASE ${config.databaseName};"
    }

    protected fun toDatabaseServerConnection(): Database {
        database = Database.connect(
            toConnectionUrl(),
            user = config.username,
            password = config.password
        )
        return database!!
    }

    override fun toDatabase(): Database {
        val database =  Database.connect(
            toDatabaseConnectionUrl(config.databaseName),
            user = config.username,
            password = config.password
        )
        this.database = database
        return database
    }

    override fun toDatabaseConnectionUrl(database: String): String {
        return toConnectionUrl() + "/$database"
    }

    override fun toConnectionUrl(): String {
        return "jdbc:mysql://${toPortedAddress()}"
    }

}