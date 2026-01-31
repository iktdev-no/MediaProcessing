package no.iktdev.mediaprocessing.coordinator.dto.rate

data class EventRate(
    val lastMinute: Long,
    val lastFiveMinutes: Long
)
