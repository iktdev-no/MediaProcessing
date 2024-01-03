package no.iktdev.mediaprocessing.shared.common.persistance

import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents.claimed
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents.claimedBy
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents.consumed
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents.created
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents.data
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents.event
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents.eventId
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents.lastCheckIn
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents.referenceId
import no.iktdev.mediaprocessing.shared.kafka.core.DeserializingRegistry
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

data class PersistentProcessDataMessage(
    val referenceId: String,
    val eventId: String,
    val event: KafkaEvents,
    val data: MessageDataWrapper,
    val created: LocalDateTime,
    val claimedBy: String? = null,
    val claimed: Boolean = false,
    val consumed: Boolean = false,
    val lastCheckIn: LocalDateTime? = null
)

fun fromRowToPersistentProcessDataMessage(row: ResultRow, dez: DeserializingRegistry): PersistentProcessDataMessage? {
    val kev = try {
        KafkaEvents.toEvent(row[event])
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        return null
    }?: return null
    val dzdata = dez.deserializeData(kev, row[data])
    return PersistentProcessDataMessage(
        referenceId = row[referenceId],
        eventId = row[eventId],
        event = kev,
        data = dzdata,
        created = row[created],
        claimed = row[claimed],
        claimedBy = row[claimedBy],
        consumed = row[consumed],
        lastCheckIn = row[lastCheckIn]
    )
}