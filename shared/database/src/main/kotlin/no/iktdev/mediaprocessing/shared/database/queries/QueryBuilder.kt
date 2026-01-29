package no.iktdev.mediaprocessing.shared.database.queries

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.andWhere

class QueryBuilder(private var query: Query) {

    fun where(condition: SqlExpressionBuilder.() -> Op<Boolean>) {
        query = query.andWhere(condition)
    }

    fun build(): Query = query
}
