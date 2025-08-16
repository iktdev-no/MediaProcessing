package no.iktdev.mediaprocessing.ui.socket

import no.iktdev.eventi.data.referenceId
import no.iktdev.eventi.database.executeWithStatus
import no.iktdev.eventi.database.toEpochSeconds
import no.iktdev.eventi.database.withDirtyRead
import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.shared.common.database.cal.toEvent
import no.iktdev.mediaprocessing.shared.common.database.tables.events
import no.iktdev.mediaprocessing.ui.dto.DatabaseEntriesDelete
import no.iktdev.mediaprocessing.ui.dto.DatabaseEventEntries
import no.iktdev.mediaprocessing.ui.eventDatabase
import no.iktdev.mediaprocessing.ui.eventsManager
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class DatabaseEntriesTopic(
    @Autowired private val template: SimpMessagingTemplate?
) {

    private fun push(destination: String, payload: Any) = template?.convertAndSend(destination, payload)
    private fun push(payload: Any) = template?.convertAndSend(payload)

    @MessageMapping("/database/events/pull")
    fun pullAllDatabaseEvents() {
        val result = withDirtyRead(eventDatabase.database) {
            events.selectAll().toEvent()
                .groupBy { it.referenceId() }.onEach { (t, u) ->
                    u.sortedBy { x -> x.metadata.created }
                }
        } ?: emptyMap()
        result.map { it -> DatabaseEventEntries(
            referenceId = it.key,
            events = it.value,
            created = it.value.first().metadata.created.toEpochSeconds() * 1000L,
            lastEventCreated = it.value.last().metadata.created.toEpochSeconds() * 1000L
        ) }.also {
            push(it)
        }
    }

    @MessageMapping("/database/events/delete")
    fun deleteEvent(@Payload payload: DatabaseEntriesDelete) {
        val status = executeWithStatus (eventDatabase.database) {
            events.deleteWhere {
                (referenceId eq payload.referenceId) and
                        (eventId eq payload.eventId)
            }
        }
        if (status) {
            pullAllDatabaseEvents()
        }
    }

    @MessageMapping("/database/tasks")
    fun pullAllDatabaseTasks() {

    }

}