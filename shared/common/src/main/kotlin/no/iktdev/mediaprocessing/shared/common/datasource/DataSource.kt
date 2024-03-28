package no.iktdev.mediaprocessing.shared.common.datasource

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

    fun toPortedAddress(): String {
        return if (!config.address.contains(":") && config.port?.isBlank() != true) {
            "$config.address:$config.port"
        } else config.address
    }

    abstract fun toDatabase(): Database

}

fun timestampToLocalDateTime(timestamp: Int): LocalDateTime {
    return Instant.ofEpochSecond(timestamp.toLong()).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

fun LocalDateTime.toEpochSeconds(): Long {
    return this.toEpochSecond(ZoneOffset.ofTotalSeconds(ZoneOffset.systemDefault().rules.getOffset(LocalDateTime.now()).totalSeconds))
}