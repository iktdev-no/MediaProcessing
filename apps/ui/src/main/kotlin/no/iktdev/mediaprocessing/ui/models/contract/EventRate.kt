package no.iktdev.mediaprocessing.ui.models.contract

data class EventRate(
    val lastMinute: Long,
    val lastFiveMinutes: Long
)