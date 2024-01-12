package no.iktdev.mediaprocessing.shared.common.datasource

import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.transactions.transaction

open class TableDefaultOperations<T : Table> {

}

fun <T> withTransaction(block: () -> T): T? {
    return try {
        transaction {
            try {
                block()
            } catch (e: Exception) {
                e.printStackTrace()
                // log the error here or handle the exception as needed
                throw e // Optionally, you can rethrow the exception if needed
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // log the error here or handle the exception as needed
        null
    }
}

fun <T> insertWithSuccess(block: () -> T): Boolean {
    return try {
        transaction {
            try {
                block()
                commit()
            } catch (e: Exception) {
                e.printStackTrace()
                // log the error here or handle the exception as needed
                throw e // Optionally, you can rethrow the exception if needed
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun <T> executeOrException(rollbackOnFailure: Boolean = false, block: () -> T): Exception? {
    return try {
        transaction {
            try {
                block()
                commit()
                null
            } catch (e: Exception) {
                // log the error here or handle the exception as needed
                if (rollbackOnFailure)
                    rollback()
                e

            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return e
    }
}

fun <T> executeWithResult(block: () -> T): Pair<T?, Exception?> {
    return try {
        transaction {
            try {
                val res = block()
                commit()
                res to null
            } catch (e: Exception) {
                // log the error here or handle the exception as needed
                rollback()
                null to e
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null to e
    }
}

fun <T> executeWithStatus(block: () -> T): Boolean {
    return try {
        transaction {
            try {
                block()
                commit()
            } catch (e: Exception) {
                e.printStackTrace()
                // log the error here or handle the exception as needed
                throw e // Optionally, you can rethrow the exception if needed
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}




