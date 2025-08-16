package no.iktdev.mediaprocessing.ui.dto

import no.iktdev.mediaprocessing.shared.common.contract.data.Event

data class DatabaseEntriesDelete(
    val referenceId: String,
    val eventId: String
)

data class DatabaseEventEntries(
    val referenceId: String,
    val events: List<Event>,
    val created: Long,
    val lastEventCreated: Long,
)