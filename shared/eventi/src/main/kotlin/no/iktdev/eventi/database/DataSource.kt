package no.iktdev.mediaprocessing.shared.common.datasource

import no.iktdev.eventi.database.DatabaseConnectionConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

abstract class DataSource(val config: DatabaseConnectionConfig) {
    open var database: Database? = null

    abstract fun connect()

    abstract fun createDatabase(): Database?

    abstract fun createTables(vararg tables: Table)

    abstract fun createDatabaseStatement(): String

    abstract fun toConnectionUrl(): String

    abstract fun toDatabaseConnectionUrl(database: String): String

    fun toPortedAddress(): String {
        var baseAddress = config.address
        if (!config.port.isNullOrBlank()) {
            baseAddress += ":${config.port}"
        }
        return baseAddress
    }

    abstract fun toDatabase(): Database

}

fun timestampToLocalDateTime(timestamp: Int): LocalDateTime {
    return Instant.ofEpochSecond(timestamp.toLong()).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

fun LocalDateTime.toEpochSeconds(): Long {
    return this.toEpochSecond(ZoneOffset.ofTotalSeconds(ZoneOffset.systemDefault().rules.getOffset(LocalDateTime.now()).totalSeconds))
}