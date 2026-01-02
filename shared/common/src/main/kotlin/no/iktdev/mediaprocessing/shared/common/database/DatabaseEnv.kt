package no.iktdev.mediaprocessing.shared.common.database

object DatabaseEnv {

    fun address() = System.getenv("DATABASE_ADDRESS") ?: "localhost"
    fun port(): String? = System.getenv("DATABASE_PORT")
    fun username() = System.getenv("DATABASE_USERNAME") ?: "root"
    fun password() = System.getenv("DATABASE_PASSWORD") ?: ""
    fun databaseName() = System.getenv("DATABASE_NAME") ?: "mediaprocessing"
    fun databaseType() = DatabaseTypes.valueOf(System.getenv("DATABASE_TYPE") ?: "MySQL")


    fun toAccess(): Access {
        val databaseType = databaseType()
        return Access(
            username = username(),
            password = password(),
            address = address(),
            port = port()?.toIntOrNull() ?: when (databaseType) {
                DatabaseTypes.MySQL -> 3306
                DatabaseTypes.PostgreSQL -> 5432
                DatabaseTypes.SQLite -> 0
                DatabaseTypes.H2 -> 0
            },
            databaseName = databaseName() ?: "streamit",
            dbType = databaseType
        )
    }

}
