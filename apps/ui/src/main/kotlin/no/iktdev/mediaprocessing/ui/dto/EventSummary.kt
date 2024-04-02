package no.iktdev.mediaprocessing.ui.dto

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents

data class EventSummary(
    val referenceId: String,
    val baseName: String? = null,
    val collection: String? = null,
    val events: List<KafkaEvents> = emptyList(),
    val status: SummaryState = SummaryState.Started,
    val activeEvens: Map<String, EventSummarySubItem>
)

data class EventSummarySubItem(
    val eventId: String,
    val status: SummaryState,
    val progress: Int = 0
)

enum class SummaryState {
    Completed,
    AwaitingStore,
    Working,
    Pending,
    AwaitingConfirmation,
    Preparing,
    Metadata,
    Analyzing,
    Read,
    Started

}