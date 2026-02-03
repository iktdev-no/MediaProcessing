package no.iktdev.mediaprocessing.transferModel.coordinatorUi

import java.time.Instant
import java.util.*

data class CoordinatorEventDto(
    val id: Long,
    val referenceId: UUID,
    val eventId: UUID,
    val event: String,
    val data: String,
    val persistedAt: Instant
)