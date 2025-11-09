package no.iktdev.mediaprocessing.shared.common.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

object DatabaseConfig {
    fun connect(
        access: Access,
        maxPoolSize: Int = 10
    ): Pair<Database, DataSource> {
        val jdbcUrl = when (access.dbType) {
            DbType.MySQL -> "jdbc:mysql://${access.address}:${access.port}/${access.databaseName}?useSSL=false&serverTimezone=UTC"
            DbType.PostgreSQL -> "jdbc:postgresql://${access.address}:${access.port}/${access.databaseName}"
            DbType.SQLite -> "jdbc:sqlite:${access.databaseName}.db"
            DbType.H2 -> "jdbc:h2:mem:${access.databaseName};DB_CLOSE_DELAY=-1"
        }

        val driver = when (access.dbType) {
            DbType.MySQL -> "com.mysql.cj.jdbc.Driver"
            DbType.PostgreSQL -> "org.postgresql.Driver"
            DbType.SQLite -> "org.sqlite.JDBC"
            DbType.H2 -> "org.h2.Driver"
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

        val dataSource = HikariDataSource(config)
        val db = Database.connect(dataSource)
        return db to dataSource
    }
}

data class Access(
    val username: String,
    val password: String,
    val address: String,
    val port: Int,
    val databaseName: String,
    val dbType: DbType
) {}