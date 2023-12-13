package no.iktdev.mediaprocessing.shared.common.persistance

import no.iktdev.mediaprocessing.shared.common.datasource.executeOrException
import no.iktdev.mediaprocessing.shared.common.datasource.executeWithStatus
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.insert
import java.sql.SQLIntegrityConstraintViolationException

open class PersistentDataStore {
    fun storeMessage(event: String, message: Message<*>): Boolean {
        val exception = executeOrException {
            events.insert {
                it[events.referenceId] = message.referenceId
                it[events.eventId] = message.eventId
                it[events.event] = event
                it[events.data] = message.dataAsJson()
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

}