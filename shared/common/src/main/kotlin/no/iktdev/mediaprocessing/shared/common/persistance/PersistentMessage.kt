package no.iktdev.mediaprocessing.shared.common.persistance

import no.iktdev.mediaprocessing.shared.kafka.core.DeserializingRegistry
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

data class PersistentMessage(
    val referenceId: String,
    val eventId: String,
    val event: KafkaEvents,
    val data: MessageDataWrapper,
    val created: LocalDateTime
)

fun fromRowToPersistentMessage(row: ResultRow, dez: DeserializingRegistry): PersistentMessage? {
    val kev = try {
        KafkaEvents.valueOf(row[events.event])
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        return null
    }
    val dzdata = dez.deserializeData(kev, row[events.data])
    return PersistentMessage(
        referenceId = row[events.referenceId],
        eventId = row[events.eventId],
        event = kev,
        data = dzdata,
        created = row[events.created]
    )
}