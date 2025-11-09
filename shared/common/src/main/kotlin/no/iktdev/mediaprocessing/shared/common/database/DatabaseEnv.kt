package no.iktdev.mediaprocessing.shared.common.database

object DatabaseEnv {
    val address: String? = System.getenv("DATABASE_ADDRESS")
    val port: String? = System.getenv("DATABASE_PORT")
    val username: String? = System.getenv("DATABASE_USERNAME")
    val password: String? = System.getenv("DATABASE_PASSWORD")
    val database: String? = System.getenv("DATABASE_NAME")
    val databaseType: DbType = DbType.valueOf(System.getenv("DATABASE_TYPE") ?: "MySQL")

    fun toAccess(): Access {
        return Access(
            username = username ?: "root",
            password = password ?: "",
            address = address ?: "localhost",
            port = port?.toIntOrNull() ?: when (databaseType) {
                DbType.MySQL -> 3306
                DbType.PostgreSQL -> 5432
                DbType.SQLite -> 0
                DbType.H2 -> 0
            },
            databaseName = database ?: "mediaprocessing",
            dbType = databaseType
        )
    }

}

enum class DbType {
    MySQL, PostgreSQL, SQLite, H2
}