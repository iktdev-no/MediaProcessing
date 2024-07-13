package no.iktdev.mediaprocessing.ui.service

import no.iktdev.mediaprocessing.shared.common.datasource.withTransaction
import no.iktdev.mediaprocessing.shared.common.persistance.events
import no.iktdev.mediaprocessing.shared.kafka.core.DeserializingRegistry
import no.iktdev.mediaprocessing.ui.getEventsDatabase
import org.jetbrains.exposed.sql.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.annotation.PostConstruct

@Service
class PersistentEventsTableService {
    val dzz = DeserializingRegistry()
    private var latestPull: LocalDateTime = LocalDateTime.MIN

    val cachedEvents: MutableMap<String, List<EventsDto>> = mutableMapOf()


    fun pullEvents() {
        val pulled = withTransaction(getEventsDatabase()) {
            val cached = latestPull
            latestPull = LocalDateTime.now()
            events.select {
                (events.created greaterEq cached)
            }
                .orderBy(events.created, SortOrder.ASC)
                .toEvent(dzz)
                .groupBy { it.referenceId }
        } ?: emptyMap()
        pulled.forEach { (rid, events) ->
            val cEvents = cachedEvents[rid] ?: emptyList()
            cachedEvents[rid] = cEvents + events
        }
    }


    fun Query?.toEvent(dzz: DeserializingRegistry): List<EventsDto> {
        return this?.mapNotNull { fromRow(it, dzz) } ?: emptyList()
    }
    fun fromRow(row: ResultRow, dez: DeserializingRegistry): EventsDto? {
        return EventsDto(
            referenceId = row[events.referenceId],
            eventId = row[events.eventId],
            event = row[events.event],
            data = row[events.data],
            created = row[events.created]
        )
    }


    @PostConstruct
    fun onInitializationCompleted() {
        pullEvents()
    }
}