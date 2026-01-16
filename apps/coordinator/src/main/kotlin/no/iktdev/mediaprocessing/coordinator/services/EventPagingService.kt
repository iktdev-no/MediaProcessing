package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.ZDS.toEvent
import no.iktdev.mediaprocessing.shared.common.dto.SequenceEvent
import no.iktdev.mediaprocessing.shared.common.dto.toDto
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.stereotype.Service
import java.util.*

@Service
class EventPagingService {

        fun getEvents(
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

}