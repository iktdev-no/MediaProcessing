package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

data class EncodeTask(
    val data: EncodeData
): Task() {
}

data class EncodeData(
    val arguments: List<String>,
    val outputFileName: String,
    val outputFolderName: String? = null,
    val inputFile: String
)