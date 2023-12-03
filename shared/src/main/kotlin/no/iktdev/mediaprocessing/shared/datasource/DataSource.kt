package no.iktdev.mediaprocessing.shared.datasource

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

abstract class DataSource(val databaseName: String, val address: String, val port: String?, val username: String, val password: String) {

    abstract fun createDatabase(): Database?

    abstract fun createTables(vararg tables: Table)

    abstract fun createDatabaseStatement(): String

    abstract fun toConnectionUrl(): String

    fun toPortedAddress(): String {
        return if (!address.contains(":") && port?.isBlank() != true) {
            "$address:$port"
        } else address
    }

}

fun timestampToLocalDateTime(timestamp: Int): LocalDateTime {
    return Instant.ofEpochSecond(timestamp.toLong()).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

fun LocalDateTime.toEpochSeconds(): Long {
    return this.toEpochSecond(ZoneOffset.ofTotalSeconds(ZoneOffset.systemDefault().rules.getOffset(LocalDateTime.now()).totalSeconds))
}