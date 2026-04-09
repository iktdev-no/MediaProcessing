package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.LineageNode
import no.iktdev.mediaprocessing.coordinator.dto.LogAssociatedIds
import no.iktdev.mediaprocessing.coordinator.toDto
import no.iktdev.mediaprocessing.shared.common.dto.EventQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.effectivePersisted
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.AlterOverrideEvent
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.DeleteResult
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.DeleteResultFailure
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceEvent
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.DeleteResultSuccess
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

    fun getAllDeletedSequences(): Set<UUID> {
        return EventStore.getAllDeletedSequences()
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

    fun getEventsLineage(referenceId: UUID): List<LineageNode> {
        val events = EventStore.getPersistedEventsFor(referenceId)
            .sortedBy { it.persistedAt }
            .effectivePersisted()
            .mapNotNull { it.toEvent() }

        return events.map { e ->
            LineageNode(
                eventId = e.eventId,
                eventName = e::class.simpleName ?: "UnknownEvent",
                parents = e.metadata.derivedFromId?.toList() ?: emptyList(),
                persistedAt = e.metadata.created
            )
        }
    }


    fun deleteEvent(referenceId: UUID, eventId: UUID): DeleteResult {
        return try {
            EventStore.deleteEvent(referenceId, eventId)
            DeleteResultSuccess()
        } catch (e: Exception) {
            DeleteResultFailure(message = e.message ?: "An error occurred")
        }
    }

    fun createOverrideRequestEvent(referenceId: UUID, taskId: UUID, derivedOf: Set<UUID>?, overrides: List<String>): Boolean {
        try {
            val alterEvent = AlterOverrideEvent(taskId, overrides)
                .apply {
                    metadata.derivedFromEventId(derivedOf ?: emptySet())
                }
                .usingReferenceId(referenceId)
            EventStore.persist(alterEvent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

}