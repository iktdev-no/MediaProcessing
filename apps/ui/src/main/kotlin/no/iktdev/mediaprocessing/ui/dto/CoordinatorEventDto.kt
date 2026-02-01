package no.iktdev.mediaprocessing.ui.dto

import java.time.Instant
import java.util.*

data class CoordinatorEventDto(
    val id: Long,
    val referenceId: UUID,
    val eventId: UUID,
    val event: String,
    val data: String,
    val persistedAt: Instant
) {
    fun toUiEvent() = UiEvent(
        id = id,
        referenceId = referenceId,
        eventId = eventId,
        event = event,
        data = data,
        persistedAt = persistedAt
    )
}