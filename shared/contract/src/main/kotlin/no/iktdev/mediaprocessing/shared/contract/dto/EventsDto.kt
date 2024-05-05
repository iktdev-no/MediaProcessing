package no.iktdev.mediaprocessing.shared.contract.dto

import java.time.LocalDateTime

data class EventsDto(
    val referenceId: String,
    val eventId: String,
    val event: String,
    val data: String,
    val created: LocalDateTime
)
