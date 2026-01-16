package no.iktdev.mediaprocessing.shared.database

import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@ConditionalOnBean(DataSource::class)
class ExposedHealthIndicator : HealthIndicator {

    override fun health(): Health {
        return try {
            transaction {
                exec("SELECT 1") { rs ->
                    if (rs.next()) rs.getInt(1) else null
                }
            }
            Health.up().build()
        } catch (e: Exception) {
            Health.down(e).build()
        }
    }
}

