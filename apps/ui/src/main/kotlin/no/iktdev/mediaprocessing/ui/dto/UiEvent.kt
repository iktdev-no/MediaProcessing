package no.iktdev.mediaprocessing.ui.dto

import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CoordinatorEventDto
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
    companion object {
        fun from(e: CoordinatorEventDto) = UiEvent(
            id = e.id,
            referenceId = e.referenceId,
            eventId = e.eventId,
            event = e.event,
            data = e.data,
            persistedAt = e.persistedAt
        )
    }
}