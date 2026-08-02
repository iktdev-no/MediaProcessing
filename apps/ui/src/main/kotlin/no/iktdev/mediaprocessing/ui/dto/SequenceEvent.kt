package no.iktdev.mediaprocessing.ui.dto

import java.time.Instant
import java.util.*

data class SequenceEvent(
    val eventId: UUID,
    val referenceId: UUID,
    val type: String,
    val timestamp: Instant,
    val metadata: MetadataDto,
    val payload: Map<String, Any?>?
)

data class MetadataDto(
    val derivedFromEventIds: Set<UUID>?,
    val createdAt: Instant
)

