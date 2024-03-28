package no.iktdev.mediaprocessing.shared.common.datasource

data class DatabaseConnectionConfig(
    val address: String,
    val port: String?,
    val username: String,
    val password: String,
    val databaseName: String
)