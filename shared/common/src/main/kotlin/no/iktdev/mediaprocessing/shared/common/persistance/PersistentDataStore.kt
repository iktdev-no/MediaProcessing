package no.iktdev.mediaprocessing.shared.common.persistance

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.datasource.DataSource
import no.iktdev.mediaprocessing.shared.common.datasource.executeOrException
import no.iktdev.mediaprocessing.shared.common.datasource.executeWithStatus
import no.iktdev.mediaprocessing.shared.common.datasource.withTransaction
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.update
import java.sql.SQLIntegrityConstraintViolationException

private val log = KotlinLogging.logger {}
open class PersistentDataStore(var dataSource: DataSource) {
    fun storeEventDataMessage(event: String, message: Message<*>): Boolean {
        val exception = executeOrException(dataSource.database) {
            events.insert {
                it[events.referenceId] = message.referenceId
                it[events.eventId] = message.eventId
                it[events.event] = event
                it[events.data] = message.dataAsJson()
            }
        }
        return if (exception == null) true else {
            if (exception.cause is SQLIntegrityConstraintViolationException) {
                log.info { "Error is of SQLIntegrityConstraintViolationException" }
                try {
                    log.info { "Error code is: ${ (exception as ExposedSQLException).errorCode}" }
                } catch (e: Exception) {

                }
                //exception.printStackTrace()
                (exception as ExposedSQLException).errorCode == 1062
            }
            else {
                exception.printStackTrace()
                false
            }
        }
    }

    fun deleteStoredEventDataMessage(referenceId: String, eventId: String, event: KafkaEvents): Boolean {
        return executeWithStatus(dataSource.database) {
            events.deleteWhere {
                (events.referenceId eq referenceId) and
                        (events.eventId eq eventId) and
                        (events.event eq event.event)
            }
        }
    }

    fun storeProcessDataMessage(event: String, message: Message<*>): Boolean {
        val exception = executeOrException(dataSource.database) {
            processerEvents.insert {
                it[processerEvents.referenceId] = message.referenceId
                it[processerEvents.eventId] = message.eventId
                it[processerEvents.event] = event
                it[processerEvents.data] = message.dataAsJson()
            }
        }
        return if (exception == null) true else {
            if (exception.cause is SQLIntegrityConstraintViolationException) {
                (exception as ExposedSQLException).errorCode == 1062
            }
            else {
                exception.printStackTrace()
                false
            }
        }
    }

    fun setProcessEventClaim(referenceId: String, eventId: String, claimedBy: String): Boolean {
        return withTransaction(dataSource.database) {
            processerEvents.update({
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId) and
                        (processerEvents.claimed eq false)
            }) {
                it[processerEvents.claimedBy] = claimedBy
                it[lastCheckIn] = CurrentDateTime
                it[claimed] = true
            }
        } == 1
    }

    fun setProcessEventCompleted(referenceId: String, eventId: String, claimedBy: String): Boolean {
        return withTransaction(dataSource.database) {
            processerEvents.update({
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId) and
                        (processerEvents.claimedBy eq claimedBy) and
                        (processerEvents.claimed eq true)
            }) {
                it[processerEvents.consumed] = true
            }
        } == 1
    }

    fun updateCurrentProcessEventClaim(referenceId: String, eventId: String, claimedBy: String): Boolean {
        return executeWithStatus(dataSource.database) {
            processerEvents.update({
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId) and
                        (processerEvents.claimed eq false) and
                        (processerEvents.claimedBy eq claimedBy)
            }) {
                it[lastCheckIn] = CurrentDateTime
            }
        }
    }

    fun releaseProcessEventClaim(referenceId: String, eventId: String): Boolean {
        val exception = executeOrException(dataSource.database) {
            processerEvents.update({
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId)
            }) {
                it[claimedBy] = null
                it[lastCheckIn] = null
                it[claimed] = false
            }
        }
        return exception == null
    }

}