package no.iktdev.mediaprocessing.shared.persistance

import no.iktdev.mediaprocessing.shared.datasource.executeWithStatus
import no.iktdev.mediaprocessing.shared.datasource.withTransaction
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import org.jetbrains.exposed.sql.insert

open class PersistentDataStore {
    fun storeMessage(event: String, message: Message<*>): Boolean {
        return executeWithStatus {
            events.insert {
                it[referenceId] = message.referenceId
                it[eventId] = message.eventId
                it[events.event] = event
                it[data] = message.dataAsJson()
            }
        }
    }

}