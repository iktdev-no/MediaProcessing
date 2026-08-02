package no.iktdev.mediaprocessing.ui.models.contract

import java.time.Instant
import java.util.*

data class UiEvent(
    val referenceId: UUID,
    val eventId: UUID,
    val event: String,
    val data: String,
    val persistedAt: Instant,
    val derivedOf: Set<UUID>?
)