package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.ZDS.toEvent
import no.iktdev.eventi.models.DeleteEvent
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.mediaprocessing.shared.common.dto.EventQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.SequenceEvent
import no.iktdev.mediaprocessing.shared.common.dto.toDto
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.stereotype.Service
import java.util.*

@Service
class EventService {

        fun getPagedEvents(
            referenceId: UUID,
            beforeEventId: UUID?,
            afterEventId: UUID?,
            limit: Int
        ): List<SequenceEvent> {

            val all = EventStore.getPersistedEventsFor(referenceId)
                .sortedByDescending { it.persistedAt }

            val filtered = when {
                beforeEventId != null ->
                    all.dropWhile { it.eventId != beforeEventId }.drop(1)

                afterEventId != null ->
                    all.takeWhile { it.eventId != afterEventId }

                else -> all
            }

            return filtered
                .take(limit)
                .mapNotNull { persisted ->
                    val event = persisted.toEvent() ?: return@mapNotNull null
                    persisted.toDto(event)
                }
        }

    fun getEvents(query: EventQuery): Paginated<PersistedEvent> {
        return EventStore.getPagedEvents(query)
    }

    fun deleteTaskFailureForReset(referenceId: UUID, taskId: UUID): UUID? {
        return EventStore.deleteFailedEventForTask(referenceId, taskId)
    }

    fun createForcedTaskResetAuditEvent(referenceId: UUID, taskId: UUID): UUID? {
        return EventStore.createTaskResetAudioEvent(referenceId, taskId)
    }

    fun getEffectiveHistory(referenceId: UUID): List<PersistedEvent> {
        val persisted = EventStore.getPersistedEventsFor(referenceId)

        // Parse alle events (kan være null hvis ukjent type)
        val parsed = persisted.mapNotNull { pe ->
            pe.toEvent()?.let { ev -> pe to ev }
        }

        // Finn alle eventIds som er slettet
        val deletedIds = parsed
            .map { it.second }
            .filterIsInstance<DeleteEvent>()
            .map { it.deletedEventId }
            .toSet()

        // Filtrer persisted basert på event-logikken
        return parsed
            .filter { (_, ev) -> ev.eventId !in deletedIds }   // fjern slettede
            .filter { (_, ev) -> ev !is DeleteEvent }          // fjern selve DeleteEvent
            .map { it.first }                                  // behold kun PersistedEvent
    }


}