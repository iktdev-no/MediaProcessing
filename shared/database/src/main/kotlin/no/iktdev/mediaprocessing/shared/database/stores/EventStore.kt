package no.iktdev.mediaprocessing.shared.database.stores

import mu.KotlinLogging
import no.iktdev.eventi.models.DeleteEvent
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.serialization.WGson
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.eventi.stores.EventStore
import no.iktdev.mediaprocessing.shared.common.UtcNow
import no.iktdev.mediaprocessing.shared.common.dto.query.EventQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.delete.DeleteSequenceEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.delete.DeletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.delete.DeletedTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.getInstancesOf
import no.iktdev.mediaprocessing.shared.common.getName
import no.iktdev.mediaprocessing.shared.common.short
import no.iktdev.mediaprocessing.shared.database.DatabaseApplication
import no.iktdev.mediaprocessing.shared.database.likeAny
import no.iktdev.mediaprocessing.shared.database.queries.ColumnSort
import no.iktdev.mediaprocessing.shared.database.queries.pagedQuery
import no.iktdev.mediaprocessing.shared.database.tables.EventsTable
import no.iktdev.mediaprocessing.shared.database.tables.EventsTable.getWhere
import no.iktdev.mediaprocessing.shared.database.withTransaction
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

object EventStore: EventStore {

    val log = KotlinLogging.logger {}
    var isDryMode: Boolean = false


    fun getPagedEvents(query: EventQuery): Paginated<PersistedEvent> =
        pagedQuery(
            table = EventsTable,
            query = query,
            sortColumns = mapOf(
                "referenceId" to ColumnSort(1, EventsTable.referenceId),
                "id" to ColumnSort(2, EventsTable.id),
                "persistedAt" to ColumnSort(3, EventsTable.persistedAt)
            ),
            applyFilters = {

                query.referenceId?.let { ref ->
                    where { EventsTable.referenceId like "%$ref%" }
                }

                query.eventId?.let { id ->
                    where { EventsTable.eventId like "%$id%" }
                }

                query.eventTypes?.let { types ->
                    where { EventsTable.event.likeAny(types) }
                }

                query.key?.takeIf { it.isNotEmpty() }?.let { keys ->
                    where { EventsTable.data.likeAny(keys) }
                }

                query.from?.let { from ->
                    where { EventsTable.persistedAt greaterEq from }
                }

                query.to?.let { to ->
                    where { EventsTable.persistedAt lessEq to }
                }
            },
            mapper = { row ->
                PersistedEvent(
                    id = row[EventsTable.id].value.toLong(),
                    referenceId = UUID.fromString(row[EventsTable.referenceId]),
                    eventId = UUID.fromString(row[EventsTable.eventId]),
                    event = row[EventsTable.event],
                    data = row[EventsTable.data],
                    persistedAt = row[EventsTable.persistedAt]
                )
            }
        )

    override fun getPersistedEventsAfter(timestamp: Instant): List<PersistedEvent> {
        return withTransaction {
            EventsTable.getWhere {
                EventsTable.persistedAt greater  timestamp
            }
        }.getOrDefault(emptyList())
    }

    override fun getPersistedEventsFor(referenceId: UUID): List<PersistedEvent> {
        if (isEventSequenceDeleted(referenceId)) return emptyList()
        val result = withTransaction {
            EventsTable
                .getWhere { EventsTable.referenceId eq referenceId.toString()}
                .sortedBy { it.id }
        }
        return result.getOrDefault(emptyList())
    }

    override fun getEventInSequence(referenceId: UUID, eventId: UUID): Event? {
        return withTransaction {
            EventsTable.getWhere {
                (EventsTable.referenceId eq referenceId.toString()) and
                        (EventsTable.eventId eq eventId.toString())
            }.single().toEvent()
        }.getOrDefault(null)
    }


    fun getPersistedEventsFor(
        referenceIds: Set<UUID>,
        eventNames: List<String>
    ): List<PersistedEvent> {
        if (referenceIds.isEmpty() || eventNames.isEmpty()) {
            return emptyList()
        }

        val deletedIds = getDeletedSequences(referenceIds)
            .map(UUID::toString)
            .toSet()

        val activeIds = referenceIds
            .map(UUID::toString)
            .filterNot(deletedIds::contains)

        if (activeIds.isEmpty()) {
            return emptyList()
        }

        val result = withTransaction {
            EventsTable.getWhere {
                (EventsTable.referenceId inList activeIds) and
                        (EventsTable.event inList eventNames)
            }
        }

        return result.getOrDefault(emptyList())
    }


    override fun persist(event: Event) {
        if (isDryMode) {
            log.warn("Event ${event.referenceId}@${event.eventId} will not be persisted as its in dry mode.")
            return
        }
        val referenceId = event.referenceId.toString()

        val asData = WGson.toJson(event)
        val eventName = event::class.simpleName ?: run {
            throw RuntimeException("Missing class name for event: $event")
        }
        withTransaction {
            EventsTable.insert {
                it[EventsTable.referenceId] = referenceId
                it[EventsTable.eventId] = event.eventId.toString()
                it[EventsTable.event] = eventName
                it[EventsTable.data] = asData
                it[EventsTable.persistedAt] = UtcNow()
            }
        }
    }

    fun persist(vararg events: Event) {
        if (isDryMode) {
            log.warn("Events ${events.map { "${it.referenceId}@${it.eventId}" }.joinToString(",")} will not be persisted as its in dry mode.")
            return
        }

        withTransaction {
            events.forEach { event ->
                val referenceId = event.referenceId.toString()
                val asData = WGson.toJson(event)
                val eventName = event::class.simpleName ?: run {
                    throw RuntimeException("Missing class name for event: $event")
                }
                EventsTable.insert {
                    it[EventsTable.referenceId] = referenceId
                    it[EventsTable.eventId] = event.eventId.toString()
                    it[EventsTable.event] = eventName
                    it[EventsTable.data] = asData
                    it[EventsTable.persistedAt] = UtcNow()
                }
            }
        }
    }

    private fun getEventSequence(referenceId: UUID): List<Event> {
        val sequenceEvents = withTransaction {
            EventsTable.getWhere {
                EventsTable.referenceId eq referenceId.toString()
            }
        }.getOrDefault(emptyList())
        return sequenceEvents.mapNotNull { it.toEvent() }
    }

    fun getEventToDelete(targetId: UUID, events: List<Event>): Event? {
        return events.find { it.metadata.derivedFromId?.any { xid -> xid == targetId } ?: false }
    }

    fun TaskResultEvent.delete(): DeleteEvent {
        val preparedDeleteEvent = DeletedTaskResultEvent(this.eventId)
            .also { usingReferenceId(this.referenceId) }
        persist(preparedDeleteEvent)
        return preparedDeleteEvent
    }

    fun Event.delete(): DeleteEvent {
        val preparedDeleteEvent = DeletedEvent(this.eventId)
            .also { usingReferenceId(this.referenceId) }
        persist(preparedDeleteEvent)
        return preparedDeleteEvent
    }

    fun Event.deleteCollectionIfPresent(events: List<Event>): DeleteEvent? {
        val collectEvents = events.getInstancesOf<CollectedEvent>().lastOrNull() ?: return null
        return if (this.eventId in collectEvents.eventIds) {
            log.info("[${this.referenceId.short()}] Found eventId (${this.eventId}) in collection (${collectEvents.eventIds}), will be deleted")
            this.delete()
        } else null
    }


    fun deleteEventForTaskResult(referenceId: UUID, taskId: UUID): UUID? {
        val serialized = getEventSequence(referenceId)
        val targetedEvent = getEventToDelete(taskId, serialized)
        if (targetedEvent == null || targetedEvent !is TaskResultEvent) {
            log.error { "TaskId $taskId does not exist within the metadata of any events within the scope of $referenceId" }
        } else {
            log.info { "Identified ${targetedEvent.eventId} in ${targetedEvent.referenceId} as being derived from $taskId" }
            val deletionEvent = targetedEvent.delete()
            targetedEvent.deleteCollectionIfPresent(serialized)
            return deletionEvent.deletedEventId
        }
        return null
    }

    fun deleteFailedTaskResultAndCreateIgnore(referenceId: UUID, taskId: UUID): Pair<UUID, UUID>? {
        val serialized = getEventSequence(referenceId)
        val targetedEvent = serialized.find { it.metadata.derivedFromId?.any { uUID -> uUID == taskId } == true }
        when (targetedEvent) {
            null -> {
                log.error { "TaskId $taskId does not exist within the metadata of any events within the scope of $referenceId" }
            }
            !is TaskResultEvent -> {
                log.error { "Event is not a type of TaskResultEvent ${targetedEvent.eventId}" }
            }
            else -> {
                log.info { "Identified ${targetedEvent.eventId} in ${targetedEvent.referenceId} as being derived from $taskId" }
                val newSkippedReference = targetedEvent.newStatus(TaskStatus.Skipped)
                val deletionEvent = targetedEvent.delete()
                persist(newSkippedReference)
                targetedEvent.deleteCollectionIfPresent(serialized)
                return deletionEvent.eventId to newSkippedReference.eventId
            }
        }
        return null
    }


    fun createTaskResetAuditEvent(referenceId: UUID, taskId: UUID): UUID {
        val auditEvent = ForcedTaskResetAuditEvent(taskId)
            .usingReferenceId(referenceId)
        persist(auditEvent)
        return auditEvent.eventId
    }

    fun createManuallyContinueEvent(referenceId: UUID): UUID? {
        val onHoldEvent = getEventSequence(referenceId).getInstancesOf<OnHoldSignalEvent>().lastOrNull()
        return try {
            val continueEvent = ReleaseHoldSignalEvent().apply {
                usingReferenceId(referenceId)
            }
            if (onHoldEvent != null) {
                continueEvent.derivedOf(onHoldEvent)
            }
            persist(continueEvent)
            return continueEvent.eventId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun eventsLast(minutes: Long = 1): Long {
        val cutoff = Instant.now().minus(minutes, ChronoUnit.MINUTES)
        return withTransaction {
            EventsTable.select(EventsTable.eventId).where {
                EventsTable.persistedAt greater cutoff
            }.count()
        }.getOrDefault(-1)
    }

    fun getIncompletedEventSequence(): List<PersistedEvent> {
        return withTransaction {
            val completedReferences = EventsTable.select(EventsTable.referenceId)
                .where { EventsTable.event eq CompletedEvent::class.getName() }
                .map { it[EventsTable.referenceId] }
            EventsTable.getWhere {
                EventsTable.referenceId notInList completedReferences
            }
        }.getOrDefault(emptyList()).groupBy { it.referenceId }
            .filterNot { (referenceId, _) -> isEventSequenceDeleted(referenceId) }.values.flatten()
    }

    fun getLastEventTimestamp(): Instant? {
        return withTransaction {
            EventsTable.select(EventsTable.persistedAt)
                .orderBy(EventsTable.persistedAt, SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.get(EventsTable.persistedAt)
        }.getOrDefault(null)
    }

    fun getEventSequenceWithLastEventAs(eventName: String): List<List<PersistedEvent>> {
        return withTransaction {
            val e = EventsTable

            // 1. Definer max-funksjonen med et tydelig alias
            val maxIdExpr = e.id.max().alias("max_id")

            // 2. Finn siste event per referenceId
            val lastEventIdForRefs = e
                .select(e.referenceId, maxIdExpr)
                .groupBy(e.referenceId)
                .alias("last_event")

            // 3. Hent ut kolonnereferansene fra ALIASET, ikke fra opprinnelig tabell
            val aliasReferenceId = lastEventIdForRefs[e.referenceId]
            val aliasMaxId = lastEventIdForRefs[maxIdExpr]

            // 4. Finn referenceId hvor siste event matcher eventName
            val completedRefs = e
                .join(lastEventIdForRefs, JoinType.INNER) {
                    // Bruk alias-kolonnene her!
                    (e.referenceId eq aliasReferenceId) and (e.id eq aliasMaxId)
                }
                .select(e.referenceId) // Vi trenger bare referenceId her
                .where { e.event eq eventName }
                .map { UUID.fromString(it[e.referenceId]) }

            if (completedRefs.isEmpty()) {
                return@withTransaction emptyList()
            }

            // 5. Hent ALLE events for ALLE disse referenceId i én query
            val allEventsForCompletedRefs = getWhere {
                e.referenceId inList completedRefs.map { it.toString() }
            }

            // 6. Gruppér per referenceId
            allEventsForCompletedRefs
                .groupBy { it.referenceId }
                .values
                .map { it.sortedBy { ev -> ev.id } }

        }.getOrDefault(emptyList())
    }

    fun getStartEvents(): List<PersistedEvent> {
        return withTransaction {
            EventsTable.getWhere { EventsTable.event eq StartProcessingEvent::class.getName() }
        }.getOrDefault(emptyList())
    }


    fun isEventSequenceDeleted(referenceId: UUID): Boolean {
        return withTransaction {
            EventsTable.getWhere {
                (EventsTable.referenceId eq referenceId.toString()) and
                        (EventsTable.event eq DeleteSequenceEvent::class.getName())
            }.count() > 0
        }.getOrDefault(false)
    }

    fun getDeletedSequences(referenceIds: Set<UUID>): Set<UUID> {
        if (referenceIds.isEmpty()) return emptySet()
        return withTransaction {
            EventsTable.select(EventsTable.referenceId)
                .where {
                    (EventsTable.referenceId inList referenceIds.map { it.toString() }) and
                            (EventsTable.event eq DeleteSequenceEvent::class.getName())
                }
                .map { UUID.fromString(it[EventsTable.referenceId]) }
                .toSet()
        }.getOrDefault(emptySet())
    }

    fun getAllDeletedSequences(): Set<UUID> =
        withTransaction {
            EventsTable
                .select(EventsTable.referenceId)
                .where { EventsTable.event eq DeleteSequenceEvent::class.getName() }
                .withDistinct(true)
                .map { UUID.fromString(it[EventsTable.referenceId]) }
                .toSet()
        }.getOrDefault(emptySet())


    fun deleteSequence(referenceId: UUID): UUID {
        val deleteSequenceEvent = DeleteSequenceEvent().usingReferenceId(referenceId)
        persist(deleteSequenceEvent)
        return deleteSequenceEvent.eventId
    }

    fun deleteEvent(referenceId: UUID, eventId: UUID): UUID {
        val deleteEvent = DeletedEvent(deletedEventId = eventId).usingReferenceId(referenceId)
        return try {
            persist(deleteEvent)
            deleteEvent.eventId
        } catch (e: Exception) {
            log.error("Could not mark $eventId@$referenceId as deleted..", e)
            throw e
        }
    }

    fun getPreservableFiles(): List<StartProcessingEvent> {
        val deletedSequences = getAllDeletedSequences().map { it.toString() }.toSet()
        return withTransaction {
            val excludedRemoved = EventsTable.select(EventsTable.referenceId)
                .where { EventsTable.event eq CompletedInputDeletedEvent::class.getName() }
                .withDistinct(true)
                .map { it[EventsTable.referenceId] }
            val ignoreIds = excludedRemoved + deletedSequences

            val events = EventsTable.getWhere {
                (EventsTable.event eq StartProcessingEvent::class.getName()) and
                        (EventsTable.referenceId notInList ignoreIds)
            }.mapNotNull { it.toEvent() }
            events.filterIsInstance<StartProcessingEvent>()
        }.getOrDefault(emptyList())
    }

    fun getFilesInSystem(): List<String> {
        val deletedSequences = getAllDeletedSequences().map { it.toString() }.toSet()
        val events = withTransaction {
            EventsTable.getWhere {
                ((EventsTable.referenceId) notInList deletedSequences.toList()) and
                        (EventsTable.event inList listOf(FileAddedEvent::class.getName(), StartProcessingEvent::class.getName()))
            }
        }.getOrDefault(emptyList()).map { it.toEvent() }
        val started = events.filterIsInstance<StartProcessingEvent>().map { it.data.fileUri }
        val added = events.filterIsInstance<FileAddedEvent>().map { it.data.fileUri }
        return started + added
    }

}