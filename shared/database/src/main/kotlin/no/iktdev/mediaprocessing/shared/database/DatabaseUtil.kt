package no.iktdev.mediaprocessing.shared.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

fun <T> withTransaction(
    rollbackOnFailure: Boolean = false,
    run: () -> T
): Result<T> {
    return try {
        val result = transaction {
            try {
                run()
            } catch (e: Exception) {
                if (rollbackOnFailure) rollback()
                throw e
            }
        }
        Result.success(result)
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}


fun Column<String>.likeAny(values: List<String>): Op<Boolean> =
    values
        .map { this like "%$it%" }
        .reduce(Op<Boolean>::or)
