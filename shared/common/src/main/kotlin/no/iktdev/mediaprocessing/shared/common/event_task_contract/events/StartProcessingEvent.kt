package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event

data class StartProcessingEvent(
    val data: StartData
): Event() {
}


data class StartData(
    val operation: Set<OperationType>,
    val flow: StartFlow = StartFlow.Auto,
    val fileUri: String,
)

fun Set<OperationType>.isOnly(operation: OperationType): Boolean {
    return this.size == 1 && this.single() == operation
}

enum class StartFlow {
    Auto,
    Manual
}

enum class OperationType {
    ExtractSubtitles,
    Encode,
    ConvertSubtitles,
    MetadataSearch
}