package no.iktdev.mediaprocessing.shared.common

import no.iktdev.eventi.database.DatabaseConnectionConfig
import no.iktdev.eventi.database.MySqlDataSource
import org.h2.jdbcx.JdbcDataSource 
import java.io.PrintWriter
import java.sql.Connection
import java.sql.SQLFeatureNotSupportedException
import java.util.logging.Logger
import javax.sql.DataSource

class H2DataSource(private val jdbcDataSource: JdbcDataSource, databaseName: String) : DataSource, MySqlDataSource(
    DatabaseConnectionConfig(
        databaseName = databaseName, address = jdbcDataSource.getUrl(), username = jdbcDataSource.user, password = "", port = null
    )
) {

    companion object {
        val connectionUrl = "jdbc:h2:test;MODE=MySQL" //"jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;"
        fun getDatasource(): JdbcDataSource  {
            val ds = JdbcDataSource()
            ds.setUrl(connectionUrl)
            ds.user = "test"
            ds.password = ""
            return ds
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
        return "CREATE SCHEMA ${config.databaseName}"
    }

    override fun toConnectionUrl(): String {
        return connectionUrl // "jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;"
    }
}
