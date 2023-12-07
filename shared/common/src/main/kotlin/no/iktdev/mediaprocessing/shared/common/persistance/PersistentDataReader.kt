package no.iktdev.mediaprocessing.shared.common.persistance

import no.iktdev.mediaprocessing.shared.common.datasource.withTransaction
import no.iktdev.mediaprocessing.shared.kafka.core.DeserializingRegistry
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

class PersistentDataReader {
    val dzz = DeserializingRegistry()

    fun getAllMessages(): List<List<PersistentMessage>> {
        val events = withTransaction {
            events.selectAll()
                .groupBy { it[events.referenceId] }
        }
        return events?.mapNotNull { it.value.mapNotNull { v -> fromRowToPersistentMessage(v, dzz) } } ?: emptyList()
    }

    fun getMessagesFor(referenceId: String): List<PersistentMessage> {
        return withTransaction {
            events.select { events.referenceId eq referenceId }
                .orderBy(events.created, SortOrder.ASC)
                .mapNotNull { fromRowToPersistentMessage(it, dzz) }
        } ?: emptyList()
    }

}