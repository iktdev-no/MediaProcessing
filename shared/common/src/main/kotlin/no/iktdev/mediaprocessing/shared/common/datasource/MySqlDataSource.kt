package no.iktdev.mediaprocessing.shared.common.datasource

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.DatabaseConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction


open class MySqlDataSource(databaseName: String, address: String, port: String = "", username: String, password: String): DataSource(databaseName =  databaseName, address =  address, port = port, username = username, password = password) {
    val log = KotlinLogging.logger {}
    var database: Database? = null
        private set

    companion object {
        fun fromDatabaseEnv(): MySqlDataSource {
            if (DatabaseConfig.database.isNullOrBlank()) throw RuntimeException("Database name is not defined in 'DATABASE_NAME'")
            if (DatabaseConfig.username.isNullOrBlank()) throw RuntimeException("Database username is not defined in 'DATABASE_USERNAME'")
            if (DatabaseConfig.address.isNullOrBlank()) throw RuntimeException("Database address is not defined in 'DATABASE_ADDRESS'")
            return MySqlDataSource(
                databaseName = DatabaseConfig.database,
                address = DatabaseConfig.address,
                port = DatabaseConfig.port ?: "",
                username = DatabaseConfig.username,
                password = DatabaseConfig.password ?: ""
            )
        }
    }

    override fun createDatabase(): Database? {
        val ok = transaction(toDatabaseServerConnection()) {
            val tmc = TransactionManager.current().connection
            val query = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '$databaseName'"
            val stmt = tmc.prepareStatement(query, true)

            val resultSet = stmt.executeQuery()
            val databaseExists = resultSet.next()

            if (!databaseExists) {
                try {
                    exec(createDatabaseStatement())
                    log.info { "Database $databaseName created." }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            } else {
                log.info { "Database $databaseName already exists." }
                true
            }
        }

        return if (ok) toDatabase() else {
            log.error { "No database to create or connect to" }
            null
        }
    }

    override fun createTables(vararg tables: Table) {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(*tables)
            log.info { "Database transaction completed" }
        }
    }

    override fun createDatabaseStatement(): String {
        return "CREATE DATABASE $databaseName"
    }

    protected fun toDatabaseServerConnection(): Database {
        database = Database.connect(
            toConnectionUrl(),
            user = username,
            password = password
        )
        return database!!
    }

    fun toDatabase(): Database {
        database = Database.connect(
            "${toConnectionUrl()}/$databaseName",
            user = username,
            password = password
        )
        return database!!
    }

    override fun toConnectionUrl(): String {
        return "jdbc:mysql://${toPortedAddress()}"
    }

}