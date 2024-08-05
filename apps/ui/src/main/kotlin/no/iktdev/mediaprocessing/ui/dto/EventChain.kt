package no.iktdev.mediaprocessing.ui.dto

data class EventChain(
    val eventId: String,
    val eventName: String,
    val elements: MutableList<EventChain> = mutableListOf()
)