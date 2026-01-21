package no.iktdev.mediaprocessing.shared.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration {

    @Bean
    fun dataSource(): DataSource {
        val maxPoolSize = 10
        val access = DatabaseEnv.toAccess()

        val jdbcUrl = when (access.dbType) {
            DatabaseTypes.MySQL -> "jdbc:mysql://${access.address}:${access.port}/${access.databaseName}?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC"
            DatabaseTypes.PostgreSQL -> "jdbc:postgresql://${access.address}:${access.port}/${access.databaseName}"
            DatabaseTypes.SQLite -> "jdbc:sqlite:${access.databaseName}.db"
            DatabaseTypes.H2 -> "jdbc:h2:mem:${access.databaseName};MODE=MySQL;DB_CLOSE_DELAY=-1"
        }

        val driver = when (access.dbType) {
            DatabaseTypes.MySQL -> "com.mysql.cj.jdbc.Driver"
            DatabaseTypes.PostgreSQL -> "org.postgresql.Driver"
            DatabaseTypes.SQLite -> "org.sqlite.JDBC"
            DatabaseTypes.H2 -> "org.h2.Driver"
        }

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.driverClassName = driver
            this.username = access.username
            this.password = access.password
            this.maximumPoolSize = maxPoolSize
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            this.validate()
        }

        return HikariDataSource(config)
    }

}

@Configuration
@EnableConfigurationProperties(FlywayProperties::class)
@ConditionalOnProperty(name = ["spring.flyway.enabled"], havingValue = "true", matchIfMissing = true)
class FlywayAutoConfig(
    private val dataSource: DataSource,
    private val props: FlywayProperties
) {

    private val log = LoggerFactory.getLogger(FlywayAutoConfig::class.java)

    @PostConstruct
    fun migrate() {
        val locations = props.locations.ifEmpty { listOf("classpath:flyway") }

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(*locations.toTypedArray())
            .baselineOnMigrate(true)
            .load()

        val pending = flyway.info().pending()
        var migrationsToApply = true
        when {
            pending.isEmpty() -> {
                log.info("ℹ️ Flyway is up to date. No migrations to apply.")
                migrationsToApply = false
            }

            else -> {
                log.info("📦 Pending migrations: ${pending.joinToString { it.script }}")
            }
        }

        val result = flyway.migrate()

        if (result.migrationsExecuted > 0) {
            log.info("✅ Applied ${result.migrationsExecuted} migration(s).")
        } else {
            if (migrationsToApply) {
                log.info("ℹ️ No migrations were applied.")
            }
        }
    }

}

@ConfigurationProperties(prefix = "spring.flyway")
data class FlywayProperties(
    var locations: List<String> = emptyList()
)