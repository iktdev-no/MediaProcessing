package no.iktdev.mediaprocessing.shared.database

import org.jetbrains.exposed.sql.TextColumnType

enum class DatabaseTypes {
    MySQL, PostgreSQL, SQLite, H2
}

class LongTextColumnType : TextColumnType() {
    override fun sqlType(): String = "LONGTEXT"
}
