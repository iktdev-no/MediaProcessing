package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.ZDS.toEvent
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.mediaprocessing.shared.common.dto.EventQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.SequenceEvent
import no.iktdev.mediaprocessing.shared.common.dto.toDto
import no.iktdev.mediaprocessing.shared.common.effectivePersisted
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.stereotype.Service
import java.time.Instant
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
        return EventStore
            .getPersistedEventsFor(referenceId)
            .effectivePersisted()
    }

    fun getIncompleteSequences(): List<PersistedEvent> {
        return EventStore.getIncompletedEventSequence()
    }

    fun getEventsLast(minutes: Long = 1): Long {
        return EventStore.eventsLast(minutes)
    }

    fun getLastEventTimestamp(): Instant? = EventStore.getLastEventTimestamp()

}