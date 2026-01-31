package no.iktdev.mediaprocessing.shared.database.stores

import mu.KotlinLogging
import no.iktdev.eventi.ZDS
import no.iktdev.eventi.ZDS.toEvent
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.stores.EventStore
import no.iktdev.mediaprocessing.shared.common.UtcNow
import no.iktdev.mediaprocessing.shared.common.dto.EventQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DeletedTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ForcedTaskResetAuditEvent
import no.iktdev.mediaprocessing.shared.common.getName
import no.iktdev.mediaprocessing.shared.database.likeAny
import no.iktdev.mediaprocessing.shared.database.queries.pagedQuery
import no.iktdev.mediaprocessing.shared.database.tables.EventsTable
import no.iktdev.mediaprocessing.shared.database.withTransaction
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


object EventStore: EventStore {
    val log = KotlinLogging.logger {}

    fun getPagedEvents(query: EventQuery): Paginated<PersistedEvent> =
        pagedQuery(
            table = EventsTable,
            query = query,
            sortColumns = mapOf(
                "referenceId" to EventsTable.referenceId,
                "eventId" to EventsTable.eventId,
                "event" to EventsTable.event,
                "persistedAt" to EventsTable.persistedAt
            ),
            applyFilters = {

                query.referenceId?.let { ref ->
                    where { EventsTable.referenceId like "%$ref%" }
                }

                query.eventId?.let { id ->
                    where { EventsTable.eventId like "%$id%" }
                }

                query.key?.let { keys ->
                    where { EventsTable.event.likeAny(keys)}
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
        val result = withTransaction {
            EventsTable.getWhere {
                EventsTable.persistedAt greater timestamp
            }
        }
        return result.getOrDefault(emptyList())
    }

    override fun getPersistedEventsFor(referenceId: UUID): List<PersistedEvent> {
        val result = withTransaction {
            EventsTable
                .getWhere { EventsTable.referenceId eq referenceId.toString()}
        }
        return result.getOrDefault(emptyList())
    }

    override fun persist(event: Event) {
        val asData = ZDS.WGson.toJson(event)
        val eventName = event::class.simpleName ?: run {
            throw RuntimeException("Missing class name for event: $event")
        }
        withTransaction {
            EventsTable.insert {
                it[EventsTable.referenceId] = event.referenceId.toString()
                it[EventsTable.eventId] = event.eventId.toString()
                it[EventsTable.event] = eventName
                it[EventsTable.data] = asData
                it[EventsTable.persistedAt] = UtcNow()
            }
        }
    }


    fun deleteFailedEventForTask(referenceId: UUID, taskId: UUID): UUID? {
        val sequenceEvents = withTransaction {
            EventsTable.getWhere {
                EventsTable.referenceId eq referenceId.toString()
            }
        }.getOrDefault(emptyList())
        val serialized = sequenceEvents.map { it.toEvent() }
        val targetedEvent = serialized.find { it?.metadata?.derivedFromId?.any { uUID -> uUID == taskId } == true }
        if (targetedEvent == null) {
            log.error { "TaskId $taskId does not exist within the metadata of any events within the scope of $referenceId" }
        } else {
            log.info { "Identified ${targetedEvent.eventId} in ${targetedEvent.referenceId} as being derived from $taskId" }
            val preparedDeleteEvent = DeletedTaskResultEvent(targetedEvent.eventId)
                .apply { usingReferenceId(targetedEvent.referenceId) }
            persist(preparedDeleteEvent)
            return preparedDeleteEvent.deletedEventId
        }
        return null
    }

    fun createTaskResetAudioEvent(referenceId: UUID, taskId: UUID): UUID {
        val auditEvent = ForcedTaskResetAuditEvent(taskId)
            .usingReferenceId(referenceId)
        persist(auditEvent)
        return auditEvent.eventId
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
        }.getOrDefault(emptyList())
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
}