package no.iktdev.mediaprocessing.ui.dto

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents

data class EventSummary(
    val referenceId: String,
    val baseName: String,
    val collection: String,
    val events: List<KafkaEvents>,
    val status: SummaryState,
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
    Reading,
    Started

}