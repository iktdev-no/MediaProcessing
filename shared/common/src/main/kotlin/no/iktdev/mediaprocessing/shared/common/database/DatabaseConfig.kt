package no.iktdev.mediaprocessing.shared.common.database

data class Access(
    val username: String,
    val password: String,
    val address: String,
    val port: Int,
    val databaseName: String,
    val dbType: DatabaseTypes
) {}