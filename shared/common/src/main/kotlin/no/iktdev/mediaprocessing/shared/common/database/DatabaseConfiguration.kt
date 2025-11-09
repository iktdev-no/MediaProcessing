package no.iktdev.mediaprocessing.shared.common.database

import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Configuration
open class DatabaseConfiguration {

    @Bean
    fun dataSource(): DataSource {
        val access = Access(
            username = "sa",
            password = "",
            address = "", // ikke brukt for H2
            port = 0,     // ikke brukt for H2
            databaseName = "testdb",
            dbType = DbType.H2
        )
        return DatabaseConfig.connect(access).second
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
        if (pending.isEmpty()) {
            log.warn("⚠️ No pending Flyway migrations found in ${locations.joinToString()}")
        } else {
            log.info("📦 Pending migrations: ${pending.joinToString { it.script }}")
        }

        flyway.migrate()
        log.info("✅ Flyway migration complete.")
    }
}

@ConfigurationProperties(prefix = "spring.flyway")
data class FlywayProperties(
    var locations: List<String> = emptyList()
)