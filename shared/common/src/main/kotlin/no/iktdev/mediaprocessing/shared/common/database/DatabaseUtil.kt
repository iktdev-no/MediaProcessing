package no.iktdev.mediaprocessing.shared.common.database

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
