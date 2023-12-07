package no.iktdev.mediaprocessing.shared.common

import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import org.h2.jdbcx.JdbcDataSource 
import java.io.PrintWriter
import java.sql.Connection
import java.sql.SQLFeatureNotSupportedException
import java.util.logging.Logger
import javax.sql.DataSource

class H2DataSource(private val jdbcDataSource: JdbcDataSource, databaseName: String) : DataSource, MySqlDataSource(databaseName = databaseName, address = jdbcDataSource.getUrl(), username = jdbcDataSource.user, password = jdbcDataSource.password) {
    companion object {
        fun fromDatabaseEnv(): H2DataSource {
            if (DatabaseConfig.database.isNullOrBlank()) throw RuntimeException("Database name is not defined in 'DATABASE_NAME'")
            return H2DataSource(
                JdbcDataSource(),
                databaseName = DatabaseConfig.database!!,
            )
        }
    }
    override fun getConnection(): Connection {
        return jdbcDataSource.connection
    }

    override fun getConnection(username: String?, password: String?): Connection {
        return jdbcDataSource.getConnection(username, password)
    }

    override fun setLoginTimeout(seconds: Int) {
        jdbcDataSource.loginTimeout = seconds
    }

    override fun getLoginTimeout(): Int {
        return jdbcDataSource.loginTimeout
    }

    override fun getLogWriter(): PrintWriter? {
        return jdbcDataSource.logWriter
    }

    override fun setLogWriter(out: PrintWriter?) {
        jdbcDataSource.logWriter = out
    }

    override fun getParentLogger(): Logger? {
        throw SQLFeatureNotSupportedException("getParentLogger is not supported")
    }

    override fun <T : Any?> unwrap(iface: Class<T>?): T {
        if (iface != null && iface.isAssignableFrom(this.javaClass)) {
            return this as T
        }
        return jdbcDataSource.unwrap(iface)
    }

    override fun isWrapperFor(iface: Class<*>?): Boolean {
        if (iface != null && iface.isAssignableFrom(this.javaClass)) {
            return true
        }
        return jdbcDataSource.isWrapperFor(iface)
    }

    override fun createDatabaseStatement(): String {
        return "CREATE SCHEMA $databaseName"
    }

    override fun toConnectionUrl(): String {
        return "jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;"
    }
}
