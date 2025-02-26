package no.iktdev.mediaprocessing.ui.dto

data class EventHolder(
    val referenceId: String,
    val fileName: String?,
    val events: List<EventChain>,
    val created: Long
)

data class EventChain(
    val eventId: String,
    val eventName: String,
    val created: Long,
    val success: Boolean,
    val failure: Boolean,
    val skipped: Boolean,
    val events: MutableList<EventChain> = mutableListOf()
)