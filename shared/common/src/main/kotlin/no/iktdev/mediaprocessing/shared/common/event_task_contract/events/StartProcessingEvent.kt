package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event

data class StartProcessingEvent(
    val data: StartData
): Event() {
}


data class StartData(
    val operation: Set<OperationType>,
    val flow: ProcessFlow = ProcessFlow.Auto,
    val fileUri: String,
)

enum class ProcessFlow {
    Auto,
    Manual
}

enum class OperationType {
    Extract,
    Encode,
    Convert
}