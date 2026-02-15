package no.iktdev.mediaprocessing.transferModel.coordinatorUi

import java.time.Instant
import java.util.UUID

data class LineageNode(
    val eventId: UUID,
    val eventName: String,
    val parents: List<UUID>,
    val persistedAt: Instant?,
)