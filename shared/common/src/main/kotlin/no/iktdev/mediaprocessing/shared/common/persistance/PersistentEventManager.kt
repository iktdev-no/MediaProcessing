package no.iktdev.mediaprocessing.shared.common.persistance

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.datasource.*
import no.iktdev.mediaprocessing.shared.kafka.core.DeserializingRegistry
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.text.Charsets.UTF_8

private val log = KotlinLogging.logger {}

class PersistentEventManager(private val dataSource: DataSource) {
    val dzz = DeserializingRegistry()


    /**
     * Deletes the events
     */
    private fun deleteSupersededEvents(superseded: List<PersistentMessage>) {
        withTransaction(dataSource) {
            superseded.forEach { duplicate ->
                events.deleteWhere {
                    (events.referenceId eq duplicate.referenceId) and
                            (events.eventId eq duplicate.eventId) and
                            (events.event eq duplicate.event.event)
                }
            }
        }
    }


    private val exemptedFromSingleEvent = listOf(
        KafkaEvents.EventWorkConvertCreated,
        KafkaEvents.EventWorkExtractCreated,
        KafkaEvents.EventWorkConvertPerformed,
        KafkaEvents.EventWorkExtractPerformed
    )

    private fun isExempted(event: KafkaEvents): Boolean {
        return event in exemptedFromSingleEvent
    }


    /**
     * @param referenceId Reference
     * @param eventId Current eventId for the message, required to prevent deletion of itself
     * @param event Current event for the message
     */
    private fun deleteSupersededEvents(referenceId: String, eventId: String, event: KafkaEvents, derivedFromId: String?) {
        val forRemoval = mutableListOf<PersistentMessage>()

        val present = getEventsWith(referenceId).filter { it.data.derivedFromEventId != null }
        val helper = PersistentMessageHelper(present)

        val replaced = if (!isExempted(event)) present.find { it.eventId != eventId && it.event == event } else null
        val orphaned = replaced?.let { helper.getEventsRelatedTo(it.eventId) } ?: emptyList()

        forRemoval.addAll(orphaned)

        //superseded.filter { !notSuperseded.contains(it) }.forEach { availableForRemoval.addAll(helper.getEventsRelatedTo(it.eventId)) }

        deleteSupersededEvents(forRemoval)

    }


    //region Database read

    fun getEventsWith(referenceId: String): List<PersistentMessage> {
        return withDirtyRead(dataSource.database) {
            events.select {
                (events.referenceId eq referenceId)
            }
                .orderBy(events.created, SortOrder.ASC)
                .toPersistentMessage(dzz)
        } ?: emptyList()
    }

    fun getAllEvents(): List<PersistentMessage> {
        return withDirtyRead(dataSource.database) {
            events.selectAll()
                .toPersistentMessage(dzz)
        } ?: emptyList()
    }

    fun getAllEventsGrouped(): List<List<PersistentMessage>> {
        return getAllEvents().toGrouped()
    }

    fun getEventsUncompleted(): List<List<PersistentMessage>> {
        val identifiesAsCompleted = listOf(
            KafkaEvents.EventCollectAndStore
        )
        val all = getAllEventsGrouped()
        return all.filter { entry -> entry.none { it.event in identifiesAsCompleted } }
    }

    //endregion

    //region Database write

    val digest = MessageDigest.getInstance("MD5")
    @OptIn(ExperimentalStdlibApi::class)
    private fun getIntegrityOfData(data : String) : String {
        return digest.digest(data.toByteArray(kotlin.text.Charsets.UTF_8))
            .toHexString()
    }

    /**
     * Stores the kafka event and its data in the database as PersistentMessage
     * @param event KafkaEvents
     * @param message Kafka message object
     */
    fun setEvent(event: KafkaEvents, message: Message<*>): Boolean {

        withTransaction(dataSource.database) {
            allEvents.insert {
                it[referenceId] = message.referenceId
                it[eventId] = message.eventId
                it[events.event] = event.event
                it[data] = message.dataAsJson()
            }
        }

        val existing = getEventsWith(message.referenceId)

        val derivedId = message.data?.derivedFromEventId
        if (derivedId != null) {
            val isNewEventOrphan = existing.none { it.eventId == derivedId }
            if (isNewEventOrphan) {
                log.warn { "Message not saved! ${message.referenceId} with eventId(${message.eventId}) for event ${event.event} has derivedEventId($derivedId) which does not exist!" }
                return false
            }
        }


        val exception = executeOrException(dataSource.database) {
            events.insert {
                it[referenceId] = message.referenceId
                it[eventId] = message.eventId
                it[events.event] = event.event
                it[data] = message.dataAsJson()
            }
        }
        val success = if (exception != null) {
            if (exception.isExposedSqlException()) {
                if ((exception as ExposedSQLException).isCausedByDuplicateError()) {
                    log.debug { "Error is of SQLIntegrityConstraintViolationException" }
                    log.error { exception.message }
                    exception.printStackTrace()
                } else {
                    log.debug { "Error code is: ${exception.errorCode}" }
                    log.error { exception.message }
                    exception.printStackTrace()
                }
            } else {
                log.error { exception.message }
                exception.printStackTrace()
            }
            false
        } else {
            true
        }
        if (success) {
            deleteSupersededEvents(referenceId = message.referenceId, eventId = message.eventId, event = event, derivedFromId = message.data?.derivedFromEventId)
        }
        return success
    }


    //endregion

}


fun List<PersistentMessage>?.toGrouped(): List<List<PersistentMessage>> {
    return this?.groupBy { it.referenceId }?.mapNotNull { it.value } ?: emptyList()
}

fun Query?.toPersistentMessage(dzz: DeserializingRegistry): List<PersistentMessage> {
    return this?.mapNotNull { fromRowToPersistentMessage(it, dzz) } ?: emptyList()
}
