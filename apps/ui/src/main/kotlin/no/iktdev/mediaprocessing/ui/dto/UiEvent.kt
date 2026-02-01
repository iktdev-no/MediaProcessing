package no.iktdev.mediaprocessing.ui.dto

import java.time.Instant
import java.util.*

data class UiEvent(
    val id: Long,
    val referenceId: UUID,
    val eventId: UUID,
    val event: String,
    val data: String,
    val persistedAt: Instant
) {
}