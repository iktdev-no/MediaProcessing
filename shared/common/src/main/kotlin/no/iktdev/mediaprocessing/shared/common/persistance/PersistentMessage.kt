package no.iktdev.mediaprocessing.shared.common.persistance

import no.iktdev.mediaprocessing.shared.kafka.core.DeserializingRegistry
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.isSkipped
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime


data class PersistentMessage(
    val referenceId: String,
    val eventId: String,
    val event: KafkaEvents,
    val data: MessageDataWrapper,
    val created: LocalDateTime
)

fun List<PersistentMessage>.lastOf(event: KafkaEvents): PersistentMessage? {
    return this.lastOrNull { it.event == event && it.isSuccess() }
}


fun PersistentMessage.isOfEvent(event: KafkaEvents): Boolean {
    return this.event == event
}

fun PersistentMessage.isSuccess(): Boolean {
    return try {
        this.data.isSuccess()
    } catch (e: Exception) {
        false
    }
}

fun PersistentMessage.isSkipped(): Boolean {
    return try {
        this.data.isSkipped()
    } catch (e: Exception) {
        false
    }
}

class PersistentMessageHelper(val messages: List<PersistentMessage>) {

    fun findOrphanedEvents(): List<PersistentMessage> {
        val withDerivedId = messages.filter { it.data.derivedFromEventId != null }
        val idsFlat = messages.map { it.eventId }
        return withDerivedId.filter { it.data.derivedFromEventId !in idsFlat }
    }

    fun getEventsRelatedTo(eventId: String): List<PersistentMessage> {
        val triggered = messages.firstOrNull { it.eventId == eventId } ?: return emptyList()
        val usableEvents = messages.filter { it.eventId != eventId && it.data.derivedFromEventId != null }

        val derivedEventsMap = mutableMapOf<String, MutableList<String>>()
        for (event in usableEvents) {
            derivedEventsMap.getOrPut(event.data.derivedFromEventId!!) { mutableListOf() }.add(event.eventId)
        }
        val eventsToDelete = mutableSetOf<String>()

        // Utfør DFS for å finne alle avledede hendelser som skal slettes
        dfs(triggered.eventId, derivedEventsMap, eventsToDelete)

        return messages.filter { it.eventId in eventsToDelete }
    }

    /**
     * @param eventId Initial eventId
     */
    private fun dfs(eventId: String, derivedEventsMap: Map<String, List<String>>, eventsToDelete: MutableSet<String>) {
        eventsToDelete.add(eventId)
        derivedEventsMap[eventId]?.forEach { derivedEventId ->
            dfs(derivedEventId, derivedEventsMap, eventsToDelete)
        }
    }
}

fun fromRowToPersistentMessage(row: ResultRow, dez: DeserializingRegistry): PersistentMessage? {
    val kev = try {
        KafkaEvents.toEvent(row[events.event])
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        return null
    }?: return null
    val dzdata = dez.deserializeData(kev, row[events.data])
    return PersistentMessage(
        referenceId = row[events.referenceId],
        eventId = row[events.eventId],
        event = kev,
        data = dzdata,
        created = row[events.created]
    )
}