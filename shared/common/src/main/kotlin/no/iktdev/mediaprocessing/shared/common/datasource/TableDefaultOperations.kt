package no.iktdev.mediaprocessing.shared.common.datasource

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.sql.SQLIntegrityConstraintViolationException

open class TableDefaultOperations<T : Table> {

}

fun <T> withDirtyRead(db: Database? = null, block: () -> T): T? {
    return try {
        transaction(db = db, transactionIsolation = Connection.TRANSACTION_READ_UNCOMMITTED) {
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

fun <T> withDirtyRead(db: DataSource? = null, block: () -> T): T? {
    return withDirtyRead(db?.database, block)
}


fun <T> withTransaction(db: Database? = null, block: () -> T): T? {
    return try {
        transaction(db) {
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
fun <T> withTransaction(db: DataSource? = null, block: () -> T): T? {
    return withTransaction(db?.database, block)
}



fun <T> insertWithSuccess(db: Database? = null, block: () -> T): Boolean {
    return try {
        transaction(db) {
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

fun <T> executeOrException(db: Database? = null, rollbackOnFailure: Boolean = false, block: () -> T): Exception? {
    return try {
        transaction(db) {
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

fun <T> executeWithResult(db: Database? = null, block: () -> T): Pair<T?, Exception?> {
    return try {
        transaction(db) {
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

fun <T> executeWithStatus(db: Database? = null, block: () -> T): Boolean {
    return try {
        transaction(db) {
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

fun <T> executeWithStatus(db: DataSource? = null, block: () -> T): Boolean {
    return executeWithStatus(db?.database, block)
}

fun Exception.isExposedSqlException(): Boolean {
    return this is ExposedSQLException
}

fun ExposedSQLException.isCausedByDuplicateError(): Boolean {
    return if (this.cause is SQLIntegrityConstraintViolationException) {
        return this.errorCode == 1062
    } else false
}


