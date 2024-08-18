package no.iktdev.mediaprocessing.shared.common.database.cal

import mu.KotlinLogging
import no.iktdev.eventi.core.PersistentMessageHelper
import no.iktdev.eventi.data.eventId
import no.iktdev.eventi.data.referenceId
import no.iktdev.eventi.data.toJson
import no.iktdev.eventi.database.DataSource
import no.iktdev.eventi.database.isCausedByDuplicateError
import no.iktdev.eventi.database.isExposedSqlException
import no.iktdev.eventi.implementations.EventsManagerImpl
import no.iktdev.mediaprocessing.shared.common.database.tables.allEvents
import no.iktdev.mediaprocessing.shared.common.database.tables.events
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.EventsManagerContract
import no.iktdev.mediaprocessing.shared.common.contract.data.Event
import no.iktdev.mediaprocessing.shared.common.contract.fromJsonWithDeserializer
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class EventsManager(dataSource: DataSource) : EventsManagerContract(dataSource) {
    val log = KotlinLogging.logger {}

    override fun storeEvent(event: Event): Boolean {

        no.iktdev.eventi.database.withTransaction(dataSource.database) {
            allEvents.insert {
                it[referenceId] = event.referenceId()
                it[eventId] = event.eventId()
                it[events.event] = event.eventType.event
                it[data] = event.toJson()
            }
        }

        val existing = getEventsWith(event.referenceId())

        val derivedId = event.metadata.derivedFromEventId
        if (derivedId != null) {
            val isNewEventOrphan = existing.none { it.eventId() == derivedId }
            if (isNewEventOrphan) {
                log.warn { "Message not saved! ${event.referenceId()} with eventId(${event.eventId()}) for event ${event.eventType} has derivedEventId($derivedId) which does not exist!" }
                return false
            }
        }


        val exception = no.iktdev.eventi.database.executeOrException(dataSource.database) {
            events.insert {
                it[referenceId] = event.referenceId()
                it[eventId] = event.eventId()
                it[events.event] = event.eventType.event
                it[data] = event.toJson()
            }
        }
        val success = if (exception != null) {
            if (exception.isExposedSqlException()) {
                if ((exception as ExposedSQLException).isCausedByDuplicateError()) {
                    log.debug { "Error is of SQLIntegrityConstraintViolationException" }
                    log.error { exception.message }
                    exception.printStackTrace()
                } else {
                    log.debug { "Error code is: ${exception.errorCode}" }
                    log.error { exception.message }
                    exception.printStackTrace()
                }
            } else {
                log.error { exception.message }
                exception.printStackTrace()
            }
            false
        } else {
            true
        }
        if (success) {
            //deleteSupersededEvents(referenceId = event.referenceId(), eventId = event.eventId(), event = event.eventType, derivedFromId = event.derivedFromEventId())
        }
        return success
    }


    private val exemptedFromSingleEvent = listOf(
        Events.EventWorkConvertCreated,
        Events.EventWorkExtractCreated,
        Events.EventWorkConvertPerformed,
        Events.EventWorkExtractPerformed
    )

    private fun isExempted(event: Events): Boolean {
        return event in exemptedFromSingleEvent
    }



    override fun readAvailableEvents(): List<List<Event>> {
        return no.iktdev.eventi.database.withTransaction(dataSource.database) {
            events.selectAll()
                .groupBy { it[events.referenceId] }
                .mapNotNull { it.value.mapNotNull { v -> v.toEvent() } }.filter { it.none { e -> e.eventType == Events.EventMediaProcessCompleted } }
        } ?: emptyList()
    }

    override fun readAvailableEventsFor(referenceId: String): List<Event> {
        val events = no.iktdev.eventi.database.withTransaction(dataSource.database) {
            events.select { events.referenceId eq referenceId }
                .mapNotNull { it.toEvent() }
        } ?: emptyList()
        return if (events.any { it.eventType == Events.EventMediaProcessCompleted  }) emptyList() else events
    }

    override fun getAllEvents(): List<List<Event>> {
        val events = no.iktdev.eventi.database.withTransaction(dataSource.database) {
            events.selectAll()
                .groupBy { it[events.referenceId] }
                .mapNotNull { it.value.mapNotNull { v -> v.toEvent() } }
        } ?: emptyList()
        return events
    }


    override fun getEventsWith(referenceId: String): List<Event> {
        return no.iktdev.eventi.database.withTransaction(dataSource.database) {
            events.select {
                (events.referenceId eq referenceId)
            }
                .orderBy(events.created, SortOrder.ASC)
                .mapNotNull { it.toEvent() }
        } ?: emptyList()
    }



    /**
     * @param referenceId Reference
     * @param eventId Current eventId for the message, required to prevent deletion of itself
     * @param event Current event for the message
     */
    private fun deleteSupersededEvents(referenceId: String, eventId: String, event: Events, derivedFromId: String?) {
        val forRemoval = mutableListOf<Event>()

        val present = getEventsWith(referenceId).filter { it.metadata.derivedFromEventId != null }
        val helper = PersistentMessageHelper<Event>(present)

        val replaced = if (!isExempted(event)) present.find { it.eventId() != eventId && it.eventType == event } else null
        val orphaned = replaced?.let { helper.getEventsRelatedTo(it.eventId()) }?.toMutableSet() ?: mutableSetOf()
        //orphaned.addAll(helper.findOrphanedEvents())

        forRemoval.addAll(orphaned)

        deleteSupersededEvents(forRemoval)

    }


    /**
     * Deletes the events
     */
    private fun deleteSupersededEvents(superseded: List<Event>) {
        no.iktdev.eventi.database.withTransaction(dataSource) {
            superseded.forEach { duplicate ->
                events.deleteWhere {
                    (referenceId eq duplicate.referenceId()) and
                            (eventId eq duplicate.eventId()) and
                            (event eq duplicate.eventType.event)
                }
            }
        }
    }


    private fun ResultRow.toEvent(): Event? {
        val kev = try {
            Events.toEvent(this[events.event])
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            return null
        }?: return null
        return this[events.data].fromJsonWithDeserializer(kev)
    }

}
