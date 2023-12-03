package no.iktdev.mediaprocessing.shared.persistance

import com.google.gson.Gson
import no.iktdev.mediaprocessing.shared.datasource.withTransaction
import no.iktdev.mediaprocessing.shared.kafka.core.DeserializingRegistry
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime

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