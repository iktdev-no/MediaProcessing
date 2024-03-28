package no.iktdev.mediaprocessing.shared.common.persistance

import no.iktdev.mediaprocessing.shared.common.datasource.DataSource
import no.iktdev.mediaprocessing.shared.common.datasource.withDirtyRead
import no.iktdev.mediaprocessing.shared.common.datasource.withTransaction
import no.iktdev.mediaprocessing.shared.kafka.core.DeserializingRegistry
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime

class PersistentDataReader(var dataSource: DataSource) {
    val dzz = DeserializingRegistry()

    fun getAllMessages(): List<List<PersistentMessage>> {
        val events = withTransaction(dataSource.database) {
            events.selectAll()
                .groupBy { it[events.referenceId] }
        }
        return events?.mapNotNull { it.value.mapNotNull { v -> fromRowToPersistentMessage(v, dzz) } } ?: emptyList()
    }

    fun getMessagesFor(referenceId: String): List<PersistentMessage> {
        return withTransaction(dataSource.database) {
            events.select { events.referenceId eq referenceId }
                .orderBy(events.created, SortOrder.ASC)
                .mapNotNull { fromRowToPersistentMessage(it, dzz) }
        } ?: emptyList()
    }

    fun getUncompletedMessages(): List<List<PersistentMessage>> {
        val result = withDirtyRead(dataSource.database) {
            events.selectAll()
                .andWhere { events.event neq KafkaEvents.EVENT_MEDIA_PROCESS_COMPLETED.event }
                .groupBy { it[events.referenceId] }
                .mapNotNull { it.value.mapNotNull { v -> fromRowToPersistentMessage(v, dzz) } }
        } ?: emptyList()
        return result
    }

    fun isProcessEventAlreadyClaimed(referenceId: String, eventId: String): Boolean {
        val result = withDirtyRead(dataSource.database) {
            processerEvents.select {
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId)
            }.mapNotNull { fromRowToPersistentProcessDataMessage(it, dzz) }.singleOrNull()
        }
        return result?.claimed ?: true
    }

    fun isProcessEventDefinedAsConsumed(referenceId: String, eventId: String, claimedBy: String): Boolean {
        return withDirtyRead(dataSource.database) {
            processerEvents.select {
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId) and
                        (processerEvents.claimedBy eq claimedBy)
            }.mapNotNull { fromRowToPersistentProcessDataMessage(it, dzz) }
        }?.singleOrNull()?.consumed ?: false
    }

    fun getAvailableProcessEvents(): List<PersistentProcessDataMessage> {
        return withDirtyRead(dataSource.database) {
            processerEvents.select {
                (processerEvents.claimed eq false) and
                        (processerEvents.consumed eq false)
            }.mapNotNull { fromRowToPersistentProcessDataMessage(it, dzz) }
        } ?: emptyList()
    }

    fun getExpiredClaimsProcessEvents(): List<PersistentProcessDataMessage> {
        val deadline = LocalDateTime.now()
        val entries = withTransaction(dataSource.database) {
            processerEvents.select {
                (processerEvents.claimed eq true) and
                        (processerEvents.consumed neq true)
            }.mapNotNull { fromRowToPersistentProcessDataMessage(it, dzz) }
        } ?: emptyList()
        return entries.filter { it.lastCheckIn == null || it.lastCheckIn.plusMinutes(15) < deadline }
    }

    fun getProcessEvent(referenceId: String, eventId: String): PersistentProcessDataMessage? {
        val message = withDirtyRead(dataSource.database) {
            processerEvents.select {
                (processerEvents.referenceId eq referenceId) and
                        (processerEvents.eventId eq eventId)
            }.mapNotNull { fromRowToPersistentProcessDataMessage(it, dzz) }
        }?.singleOrNull()
        return message
    }

    fun getProcessEvents(): List<PersistentProcessDataMessage> {
        return withTransaction(dataSource.database) {
            processerEvents.selectAll()
                .mapNotNull { fromRowToPersistentProcessDataMessage(it, dzz) }
        } ?: emptyList()
    }


}