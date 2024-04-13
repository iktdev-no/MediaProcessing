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
import java.time.LocalDateTime

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


    /**
     * @param referenceId Reference
     * @param eventId Current eventId for the message, required to prevent deletion of itself
     * @param event Current event for the message
     */
    private fun deleteSupersededEvents(referenceId: String, eventId: String, event: KafkaEvents) {
        val present = getEventsWith(referenceId).filter { it.eventId != eventId }

        val superseded = present.filter { it.event == event && it.eventId != eventId }
        val availableForRemoval = mutableListOf<PersistentMessage>()
        val helper = PersistentMessageHelper(present)
        superseded.forEach { availableForRemoval.addAll(helper.getCascadingFrom(it.eventId)) }

        deleteSupersededEvents(availableForRemoval)

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

    fun getProcessEventWith(referenceId: String, eventId: String): PersistentProcessDataMessage? {
        return withDirtyRead(dataSource.database) {
            processerEvents.select {
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId)
            }.toPersistentProcesserMessage(dzz)
        }?.singleOrNull()
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

    fun getAllProcessEvents(): List<PersistentProcessDataMessage> {
        return withDirtyRead(dataSource.database) {
            processerEvents.selectAll()
                .toPersistentProcesserMessage(dzz)
        } ?: emptyList()
    }

    fun getEventsUncompleted(): List<List<PersistentMessage>> {
        val identifiesAsCompleted = listOf(
            KafkaEvents.EventRequestProcessCompleted,
            KafkaEvents.EventMediaProcessCompleted,
            KafkaEvents.EventCollectAndStore
        )
        val all = getAllEventsGrouped()
        return all.filter { entry -> entry.none { it.event in identifiesAsCompleted } }
    }

    fun getProcessEventsUncompleted(): List<PersistentProcessDataMessage> {
        return withTransaction(dataSource.database) {
            processerEvents.select {
                (processerEvents.consumed eq false)
            }.toPersistentProcesserMessage(dzz)
        } ?: emptyList()
    }

    fun getProcessEventsClaimable(): List<PersistentProcessDataMessage> {
        return withTransaction(dataSource.database) {
            processerEvents.select {
                (processerEvents.consumed eq false) and
                        (processerEvents.claimed eq false)
            }.toPersistentProcesserMessage(dzz)
        } ?: emptyList()
    }

    fun getProcessEventsWithExpiredClaim(): List<PersistentProcessDataMessage> {
        val deadline = LocalDateTime.now()
        return getProcessEventsUncompleted()
            .filter { it.claimed && if (it.lastCheckIn != null) it.lastCheckIn.plusMinutes(15) < deadline else true }
    }

    fun isProcessEventClaimed(referenceId: String, eventId: String): Boolean {
        return getProcessEventWith(referenceId, eventId)?.claimed ?: false
    }

    fun isProcessEventCompleted(referenceId: String, eventId: String): Boolean {
        return getProcessEventWith(referenceId, eventId)?.consumed ?: false
    }

    //endregion

    //region Database write

    /**
     * Stores the kafka event and its data in the database as PersistentMessage
     * @param event KafkaEvents
     * @param message Kafka message object
     */
    fun setEvent(event: KafkaEvents, message: Message<*>): Boolean {
        val existing = getEventsWith(message.referenceId)
        val derivedId = message.data?.derivedFromEventId
        if (derivedId != null) {
            val isNewEventOrphan = existing.none { it.eventId == derivedId }
            if (isNewEventOrphan) {
                log.warn { "Message not saved! ${message.referenceId} with eventId(${message.eventId}) has derivedEventId($derivedId) which does not exist!" }
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
                    log.info { "Error is of SQLIntegrityConstraintViolationException" }
                } else {
                    log.info { "Error code is: ${exception.errorCode}" }
                    exception.printStackTrace()
                }
            } else {
                exception.printStackTrace()
            }
            false
        } else {
            true
        }
        if (success) {
            deleteSupersededEvents(referenceId = message.referenceId, eventId = message.eventId, event = event)
        }
        return success
    }

    fun setProcessEvent(event: KafkaEvents, message: Message<*>): Boolean {
        val exception = executeOrException(dataSource.database) {
            processerEvents.insert {
                it[processerEvents.referenceId] = message.referenceId
                it[processerEvents.eventId] = message.eventId
                it[processerEvents.event] = event.event
                it[processerEvents.data] = message.dataAsJson()
            }
        }
        return if (exception != null) {
            if (exception.isExposedSqlException()) {
                if ((exception as ExposedSQLException).isCausedByDuplicateError()) {
                    log.info { "Error is of SQLIntegrityConstraintViolationException" }
                } else {
                    log.info { "Error code is: ${exception.errorCode}" }
                    exception.printStackTrace()
                }
            }
            false
        } else {
            true
        }
    }

    fun setProcessEventClaim(referenceId: String, eventId: String, claimer: String): Boolean {
        return executeWithStatus(dataSource.database) {
            processerEvents.update({
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId) and
                        (processerEvents.claimed eq false) and
                        (processerEvents.consumed eq false)
            }) {
                it[claimedBy] = claimer
                it[lastCheckIn] = CurrentDateTime
                it[claimed] = true
            }
        }
    }

    fun setProcessEventCompleted(referenceId: String, eventId: String, status: Status = Status.COMPLETED): Boolean {
        return executeWithStatus(dataSource) {
            processerEvents.update({
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId)
            }) {
                it[consumed] = true
                it[claimed] = true
                it[processerEvents.status] = status.name
            }
        }
    }

    fun setProcessEventClaimRefresh(referenceId: String, eventId: String, claimer: String): Boolean {
        return executeWithStatus(dataSource) {
            processerEvents.update({
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId) and
                        (processerEvents.claimed eq true) and
                        (processerEvents.claimedBy eq claimer)
            }) {
                it[lastCheckIn] = CurrentDateTime
            }
        }
    }

    /**
     * Removes the claim set on the process event
     */
    fun deleteProcessEventClaim(referenceId: String, eventId: String): Boolean {
        return executeWithStatus(dataSource) {
            processerEvents.update({
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId)
            }) {
                it[claimed] = false
                it[claimedBy] = null
                it[lastCheckIn] = null
            }
        }
    }

    fun deleteProcessEvent(referenceId: String, eventId: String): Boolean {
        return executeWithStatus (dataSource) {
            processerEvents.deleteWhere {
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId)
            }
        }
    }

    //endregion

}


fun List<PersistentMessage>?.toGrouped(): List<List<PersistentMessage>> {
    return this?.groupBy { it.referenceId }?.mapNotNull { it.value } ?: emptyList()
}

fun Query?.toPersistentMessage(dzz: DeserializingRegistry): List<PersistentMessage> {
    return this?.mapNotNull { fromRowToPersistentMessage(it, dzz) } ?: emptyList()
}

fun Query?.toPersistentProcesserMessage(dzz: DeserializingRegistry): List<PersistentProcessDataMessage> {
    return this?.mapNotNull { fromRowToPersistentProcessDataMessage(it, dzz) } ?: emptyList()
}