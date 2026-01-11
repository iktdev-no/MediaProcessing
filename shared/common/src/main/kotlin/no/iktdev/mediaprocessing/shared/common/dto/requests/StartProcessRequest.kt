package no.iktdev.mediaprocessing.shared.common.dto.requests

import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType

data class StartProcessRequest(
    val fileUri: String,
    val operationTypes: Set<OperationType>
) {
}