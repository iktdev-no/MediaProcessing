package no.iktdev.mediaprocessing.transferModel.coordinatorUi

import java.time.Instant
import java.util.*

data class UiEvent(
    val referenceId: UUID,
    val eventId: UUID,
    val event: String,
    val data: String,
    val persistedAt: Instant
) {
    companion object {
        fun from(e: CoordinatorEventDto) = UiEvent(
            referenceId = e.referenceId,
            eventId = e.eventId,
            event = e.event,
            data = e.data,
            persistedAt = e.persistedAt
        )
    }
}