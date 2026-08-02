package no.iktdev.mediaprocessing.ui.models.contract

import java.time.Instant
import java.util.UUID

data class LineageNode(
    val eventId: UUID,
    val eventName: String,
    val parents: List<UUID>,
    val persistedAt: Instant?,
)