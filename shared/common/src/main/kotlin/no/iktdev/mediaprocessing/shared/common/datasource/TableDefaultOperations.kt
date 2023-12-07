package no.iktdev.mediaprocessing.shared.common.datasource

import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.transactions.transaction

open class TableDefaultOperations<T: Table> {

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

fun <T> executeWithStatus(block: () -> T): Boolean {
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
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}




