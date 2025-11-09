package no.iktdev.mediaprocessing.shared.common

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.database.Access
import no.iktdev.mediaprocessing.shared.common.database.DatabaseConfig
import no.iktdev.mediaprocessing.shared.common.database.DbType
import no.iktdev.mediaprocessing.shared.common.database.withTransaction
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.statements.jdbc.JdbcConnectionImpl
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class FlywayMigrationTest {

    private val log = KotlinLogging.logger {}


    @Test
    fun `should run flyway migrations and create expected tables`() {
        val access = Access(
            username = "sa",
            password = "",
            address = "", // ikke brukt for H2
            port = 0,     // ikke brukt for H2
            databaseName = "testdb",
            dbType = DbType.H2
        )

        val connection = DatabaseConfig.connect(access)

        val flyway = Flyway.configure()
            .dataSource(connection.second)
            .locations("classpath:flyway")
            .baselineOnMigrate(true)
            .load()

        // Flyway migrering
        flyway.migrate()

        // Verifiser at tabellene finnes

        withTransaction {
            val jdbc = (TransactionManager.current().connection as JdbcConnectionImpl).connection

            val meta = jdbc.metaData
            val eventsExists = meta.getTables(null, null, "EVENTS", null).next()
            val tasksExists = meta.getTables(null, null, "TASKS", null).next()

            assertTrue(eventsExists, "Events table should exist")
            assertTrue(tasksExists, "Tasks table should exist")

            log.info { "Found migrations: ${flyway.info().all().map { it.script }}" }
        }
    }
}
