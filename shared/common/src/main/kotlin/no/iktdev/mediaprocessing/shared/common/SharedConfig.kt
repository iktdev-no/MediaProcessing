package no.iktdev.mediaprocessing.shared.common

import no.iktdev.mediaprocessing.shared.common.datasource.DatabaseConnectionConfig
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import java.io.File

object SharedConfig {
    var incomingContent: File = if (!System.getenv("DIRECTORY_CONTENT_INCOMING").isNullOrBlank()) File(System.getenv("DIRECTORY_CONTENT_INCOMING")) else File("/src/input")
    val outgoingContent: File = if (!System.getenv("DIRECTORY_CONTENT_OUTGOING").isNullOrBlank()) File(System.getenv("DIRECTORY_CONTENT_OUTGOING")) else File("/src/output")

    val ffprobe: String = System.getenv("SUPPORTING_EXECUTABLE_FFPROBE") ?: "ffprobe"
    val ffmpeg: String = System.getenv("SUPPORTING_EXECUTABLE_FFMPEG") ?: "ffmpeg"
    val uiUrl: String = System.getenv("APP_URL_UI") ?: "http://ui:8080"

    val preference: File = File("/data/config/preference.json")
    val verbose: Boolean = System.getenv("VERBOSE")?.let { it.toBoolean() } ?: false
}

object DatabaseEnvConfig {
    val address: String? = System.getenv("DATABASE_ADDRESS")
    val port: String? = System.getenv("DATABASE_PORT")
    val username: String? = System.getenv("DATABASE_USERNAME")
    val password: String? = System.getenv("DATABASE_PASSWORD")
    val eventBasedDatabase: String? = System.getenv("DATABASE_NAME_E")
    val storedDatabase: String? = System.getenv("DATABASE_NAME_S")
}

fun DatabaseEnvConfig.toStoredDatabase(): MySqlDataSource {
    return MySqlDataSource(DatabaseConnectionConfig(
        databaseName = this.storedDatabase ?: "streamit",
        address = this.address ?: "localhost",
        port = this.port,
        username = this.username ?: "root",
        password = this.password ?: ""
    ))
}

fun DatabaseEnvConfig.toEventsDatabase(): MySqlDataSource {
    return MySqlDataSource(DatabaseConnectionConfig(
        databaseName = this.eventBasedDatabase ?: "persistentEvents",
        address = this.address ?: "localhost",
        port = this.port,
        username = this.username ?: "root",
        password = this.password ?: ""
    ))
}