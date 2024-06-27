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

    @Deprecated("Use PersistentEventManager.getAllEventsGrouped")
    fun getAllMessages(): List<List<PersistentMessage>> {
        val events = withTransaction(dataSource.database) {
            events.selectAll()
                .groupBy { it[events.referenceId] }
        }
        return events?.mapNotNull { it.value.mapNotNull { v -> fromRowToPersistentMessage(v, dzz) } } ?: emptyList()
    }

    @Deprecated("Use PersistentEventManager.getEvetnsWith")
    fun getMessagesFor(referenceId: String): List<PersistentMessage> {
        return withTransaction(dataSource.database) {
            events.select { events.referenceId eq referenceId }
                .orderBy(events.created, SortOrder.ASC)
                .mapNotNull { fromRowToPersistentMessage(it, dzz) }
        } ?: emptyList()
    }

    @Deprecated("Use PersistentEventManager.getEventsUncompleted")
    fun getUncompletedMessages(): List<List<PersistentMessage>> {
        val result = withDirtyRead(dataSource.database) {
            events.selectAll()
                .andWhere { events.event neq KafkaEvents.EventMediaProcessCompleted.event }
                .groupBy { it[events.referenceId] }
                .mapNotNull { it.value.mapNotNull { v -> fromRowToPersistentMessage(v, dzz) } }
        } ?: emptyList()
        return result
    }





}