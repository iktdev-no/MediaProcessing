package no.iktdev.mediaprocessing.shared.common.persistance

import no.iktdev.mediaprocessing.shared.common.datasource.executeWithStatus
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import org.jetbrains.exposed.sql.insert

open class PersistentDataStore {
    fun storeMessage(event: String, message: Message<*>): Boolean {
        return executeWithStatus {
            events.insert {
                it[events.referenceId] = message.referenceId
                it[events.eventId] = message.eventId
                it[events.event] = event
                it[events.data] = message.dataAsJson()
            }
        }
    }

}