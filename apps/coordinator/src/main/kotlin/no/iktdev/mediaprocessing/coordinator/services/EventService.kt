package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.ZDS.toEvent
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.mediaprocessing.coordinator.dto.LogAssociatedIds
import no.iktdev.mediaprocessing.coordinator.toDto
import no.iktdev.mediaprocessing.shared.common.dto.EventQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.effectivePersisted
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DeletedEvent
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.DeleteResult
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.Failure
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceEvent
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.Success
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
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
        return EventStore.createTaskResetAuditEvent(referenceId, taskId)
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
    fun isSequenceDeleted(referenceId: UUID): Boolean {
        return EventStore.isEventSequenceDeleted(referenceId)
    }
    fun getDeletedSequences(referenceIds: Set<UUID>): Set<UUID> {
        return EventStore.getDeletedSequences(referenceIds)
    }

    val taskResultEventTypes: List<String> =
        EventRegistry.getEvents()
            .filter { TaskResultEvent::class.java.isAssignableFrom(it) }
            .map { it.simpleName }


    fun getTaskEventResultsWithLogs(referenceIds: Set<UUID>): List<LogAssociatedIds> {
        // 1. Hent persisted events som matcher TaskResultEvent-typene
        val persisted = EventStore.getPersistedEventsFor(referenceIds, taskResultEventTypes)

        // 2. Deserialiser til domeneklasse
        val domainEvents = persisted.map { it.toEvent() }

        // 3. Filtrer til TaskResultEvent-instansene som har logg
        return domainEvents
            .filterIsInstance<TaskResultEvent>()
            .filter { it.logFile != null }
            .map {
                LogAssociatedIds(
                    referenceId = it.referenceId,
                    ids = setOf( it.eventId, *(it.metadata.derivedFromId?.toTypedArray() ?: emptyArray())),
                    logFile = it.logFile!!
                )
            }
    }

    fun deleteEvent(referenceId: UUID, eventId: UUID): DeleteResult {
        return try {
            EventStore.deleteEvent(referenceId, eventId)
            Success()
        } catch (e: Exception) {
            Failure(message = e.message ?: "An error occurred")
        }
    }


}