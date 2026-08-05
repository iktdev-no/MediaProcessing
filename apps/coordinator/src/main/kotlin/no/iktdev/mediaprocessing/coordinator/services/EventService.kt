package no.iktdev.mediaprocessing.coordinator.services

import mu.KotlinLogging
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.mediaprocessing.shared.common.dto.processer.LogAssociatedIds
import no.iktdev.mediaprocessing.shared.common.dto.query.EventQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.effectivePersisted
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.AlterOverrideEvent
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class EventService {

    private val log = KotlinLogging.logger {}


    fun getEvents(query: EventQuery): Paginated<PersistedEvent> {
        return EventStore.getPagedEvents(query)
    }

    fun deleteTaskFailureForReset(referenceId: UUID, taskId: UUID): UUID? {
        return EventStore.deleteEventForTaskResult(referenceId, taskId)
    }

    fun deleteTaskResultForIgnore(referenceId: UUID, taskId: UUID): Pair<UUID, UUID>? {
        return EventStore.deleteFailedTaskResultAndCreateIgnore(referenceId, taskId)
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


    fun deleteEvent(referenceId: UUID, eventId: UUID): Boolean {
        return try {
            EventStore.deleteEvent(referenceId, eventId)
            true
        } catch (e: Exception) {
            log.error("Failed to delete event $eventId with reason $e", e)
            false
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