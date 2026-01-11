package no.iktdev.mediaprocessing.shared.common.dto

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KProperty1

data class SequenceEvent(
    val eventId: UUID,
    val referenceId: UUID,
    val type: String,
    val timestamp: LocalDateTime,
    val metadata: MetadataDto,
    val payload: Map<String, Any?>?
)

data class MetadataDto(
    val derivedFromEventIds: Set<UUID>?,
    val createdAt: LocalDateTime
)

fun Event.extractPayload(): Map<String, Any?>? {
    val ignored = setOf("referenceId", "eventId", "metadata")

    return this::class.members
        .filterIsInstance<KProperty1<Event, *>>()
        .filter { it.name !in ignored }
        .associate { it.name to it.get(this) }
}


fun PersistedEvent.toDto(event: Event): SequenceEvent =
    SequenceEvent(
        eventId = this.eventId,
        referenceId = this.referenceId,
        type = this.event,
        timestamp = this.persistedAt,
        metadata = MetadataDto(
            derivedFromEventIds = event.metadata.derivedFromId,
            createdAt = event.metadata.created
        ),
        payload = event.extractPayload()
    )
