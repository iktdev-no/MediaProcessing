package no.iktdev.mediaprocessing.shared.database

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.TestBase
import no.iktdev.mediaprocessing.shared.database.config.DatasourceConfiguration
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.statements.jdbc.JdbcConnectionImpl
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import javax.sql.DataSource

@SpringBootTest(
    classes = [
        TestDatabaseApplication::class,
        DatasourceConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TestBaseWithDatabase() {
    val log = KotlinLogging.logger {}

    var validToken: String? = null


    @Autowired
    lateinit var dataSource: DataSource

    lateinit var database: Database
    private lateinit var flyway: Flyway


    @BeforeAll
    fun setupDatabase() {
        val access = Access(
            username = "sa",
            password = "",
            address = "", // ikke brukt for H2
            port = 0,     // ikke brukt for H2
            databaseName = "testdb",
            dbType = DatabaseTypes.H2
        )
        database = Database.Companion.connect(dataSource)
        flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:flyway")
            .cleanDisabled(false)
            .load()

        flyway.clean()
        flyway.migrate()


        withTransaction {
            val jdbc = (TransactionManager.Companion.current().connection as JdbcConnectionImpl).connection

            val meta = jdbc.metaData
            val tableNames = listOf<String>(
                "EVENTS",
                "TASKS"
            )
            val existingTables = tableNames.map {
                it to meta.getTables(null, null, it.uppercase(), null).next()
            }

            existingTables.forEach { (tableName, exists) ->
                Assertions.assertTrue(exists, "Table $tableName should exist after migration")
            }

            log.info { "Found migrations: ${flyway.info().all().map { it.script }}" }
        }
    }

    @AfterAll
    fun clearDatabase() {
        flyway.clean()
        TransactionManager.Companion.closeAndUnregister(database)
    }

}